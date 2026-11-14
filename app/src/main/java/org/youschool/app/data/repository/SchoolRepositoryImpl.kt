package org.youschool.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.youschool.app.core.auth.AuthManager
import org.youschool.app.core.database.YouSchoolDatabase
import org.youschool.app.core.database.entities.GradeEntity
import org.youschool.app.core.database.entities.HomeworkStatusEntity
import org.youschool.app.core.database.entities.LessonEntity
import org.youschool.app.core.network.EschoolApiClient
import org.youschool.app.core.network.dto.FileDto
import org.youschool.app.domain.model.*
import org.youschool.app.domain.repository.SchoolRepository
import java.util.*

class SchoolRepositoryImpl(
    private val database: YouSchoolDatabase,
    private val apiClient: EschoolApiClient,
    private val authManager: AuthManager
) : SchoolRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getLessonsForDay(dateMillis: Long): Flow<List<Lesson>> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        val lessonsFlow = database.lessonDao().getLessonsForDay(startOfDay, endOfDay)
        val statusFlow = database.homeworkStatusDao().getAllStatuses()

        return combine(lessonsFlow, statusFlow) { entities, statuses ->
            val statusMap = statuses.associateBy { it.lessonId }
            entities.map { it.toDomain(statusMap[it.id]?.isCompleted ?: false) }
        }
    }

    override fun getAllHomeworks(): Flow<List<Lesson>> {
        val lessonsFlow = database.lessonDao().getAllLessonsWithHomework()
        val statusFlow = database.homeworkStatusDao().getAllStatuses()

        return combine(lessonsFlow, statusFlow) { entities, statuses ->
            val statusMap = statuses.associateBy { it.lessonId }
            entities.map { it.toDomain(statusMap[it.id]?.isCompleted ?: false) }
        }
    }

    override fun getAllGrades(): Flow<List<Grade>> {
        return database.gradeDao().getAllGrades().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSubjectAverages(): Flow<List<SubjectAverage>> {
        return database.gradeDao().getAllGrades().map { entities ->
            val grouped = entities.groupBy { it.subject }
            grouped.map { (subject, grades) ->
                var totalWeightedScore = 0.0
                var totalWeight = 0
                for (g in grades) {
                    if (g.markNum > 0) {
                        totalWeightedScore += g.markNum * g.weight
                        totalWeight += g.weight
                    }
                }
                val avg = if (totalWeight > 0) totalWeightedScore / totalWeight else 0.0
                // Calculate how many 5s needed to reach 4.60 (an A / 5)
                var neededFives = 0
                if (avg in 1.0..4.59 && totalWeight > 0) {
                    var simulatedTotal = totalWeightedScore
                    var simulatedWeight = totalWeight
                    while ((simulatedTotal / simulatedWeight) < 4.60 && neededFives < 30) {
                        simulatedTotal += 5.0
                        simulatedWeight += 1
                        neededFives++
                    }
                }

                SubjectAverage(
                    subjectName = subject,
                    averageScore = avg,
                    gradesCount = grades.size,
                    targetFiveNeeded = neededFives
                )
            }.sortedBy { it.subjectName }
        }
    }

    override suspend fun syncDayLessons(dateMillis: Long): Result<Unit> {
        val userId = authManager.getUserId()
        if (userId <= 0) return Result.failure(Exception("Пользователь не авторизован"))

        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        return try {
            val response = apiClient.getDiary(userId, startOfDay, endOfDay)
            if (response.isSuccess) {
                val lessonsDto = response.getOrNull()?.lesson ?: emptyList()
                val entities = lessonsDto.map { dto ->
                    var homeworkText = ""
                    val filesList = mutableListOf<FileDto>()

                    for (part in dto.part) {
                        if (part.cat == "DZ") {
                            for (v in part.variant) {
                                if (!v.text.isNullOrBlank()) {
                                    homeworkText += (if (homeworkText.isNotEmpty()) "\n" else "") + v.text
                                }
                                filesList.addAll(v.file)
                            }
                        }
                    }

                    LessonEntity(
                        id = dto.id,
                        dateMillis = dto.date.toLongOrNull() ?: startOfDay,
                        numInDay = dto.numInDay,
                        subjectId = dto.unit?.id,
                        subjectName = dto.unit?.name ?: "Урок",
                        subjectShort = dto.unit?.short ?: dto.unit?.name ?: "Урок",
                        topic = dto.tp?.topicName ?: "",
                        teacherName = dto.teacher?.factTeacherIN ?: "",
                        homeworkText = homeworkText,
                        homeworkFilesJson = json.encodeToString(filesList),
                        gradeValue = null,
                        gradeWeight = null
                    )
                }

                database.lessonDao().deleteLessonsForDay(startOfDay, endOfDay)
                database.lessonDao().insertLessons(entities)
                Result.success(Unit)
            } else {
                Result.failure(response.exceptionOrNull() ?: Exception("Ошибка загрузки расписания"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncGrades(): Result<Unit> {
        val userId = authManager.getUserId()
        if (userId <= 0) return Result.failure(Exception("Пользователь не авторизован"))

        return try {
            // Запрашиваем текущие периоды
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val periodsResult = apiClient.getPeriods(currentYear)
            var currentPeriodId: Long = 0

            if (periodsResult.isSuccess) {
                val containers = periodsResult.getOrNull() ?: emptyList()
                val now = System.currentTimeMillis()
                for (container in containers) {
                    for (item in container.items) {
                        if (now in item.date1..item.date2 && item.typeCode == "Q") {
                            currentPeriodId = item.id
                            break
                        }
                    }
                }
            }

            val gradesResult = apiClient.getGradesForPeriod(userId, currentPeriodId)
            if (gradesResult.isSuccess) {
                val gradesDto = gradesResult.getOrNull()?.result ?: emptyList()
                val entities = gradesDto.map { dto ->
                    GradeEntity(
                        lessonId = dto.lessonId,
                        unitId = dto.unitId,
                        subject = dto.subject ?: "Предмет",
                        weight = if (dto.mktWt > 0) dto.mktWt else 1,
                        markVal = dto.markVal ?: "",
                        markNum = dto.markNum ?: dto.markVal?.toDoubleOrNull() ?: 0.0,
                        markDate = dto.markDate ?: "",
                        workType = dto.lptName ?: "",
                        teacherFio = dto.teachFio ?: ""
                    )
                }
                database.gradeDao().clearAllGrades()
                database.gradeDao().insertGrades(entities)
                Result.success(Unit)
            } else {
                Result.failure(gradesResult.exceptionOrNull() ?: Exception("Ошибка загрузки оценок"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleHomeworkStatus(lessonId: Long, isDone: Boolean) {
        database.homeworkStatusDao().setStatus(HomeworkStatusEntity(lessonId, isDone))
    }

    override suspend fun login(login: String, rawPassword: String): Result<UserProfile> {
        val hash = AuthManager.sha256(rawPassword)
        val result = apiClient.login(login, hash)
        return if (result.isSuccess) {
            val state = result.getOrNull()
            val userId = state?.userId ?: state?.user?.userId ?: -1L
            val prsId = state?.user?.prsId ?: -1L
            val firstName = state?.profile?.firstName ?: ""
            val lastName = state?.profile?.lastName ?: ""
            val fullName = "$lastName $firstName".trim().ifEmpty { login }
            val school = state?.user?.currentPosition?.orgName ?: "Школа"
            val role = state?.user?.roles?.firstOrNull() ?: "ROLE_STUDENT"

            authManager.saveCredentials(login, rawPassword)
            authManager.saveUserInfo(userId, prsId, fullName, school)

            val profile = UserProfile(userId, prsId, fullName, school, role)
            Result.success(profile)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Ошибка авторизации"))
        }
    }

    override fun isUserLoggedIn(): Boolean = authManager.hasCredentials()

    override fun getUserProfile(): UserProfile? {
        val userId = authManager.getUserId()
        if (userId <= 0) return null
        return UserProfile(
            userId = userId,
            prsId = authManager.getPrsId(),
            fullName = authManager.getFullName(),
            schoolName = authManager.getSchoolName(),
            role = "ROLE_STUDENT"
        )
    }

    override fun logout() {
        authManager.clear()
    }

    private fun LessonEntity.toDomain(isDone: Boolean): Lesson {
        val files: List<HomeworkAttachment> = try {
            val dtos = json.decodeFromString<List<FileDto>>(homeworkFilesJson)
            dtos.map { HomeworkAttachment(it.id, it.fileName) }
        } catch (e: Exception) {
            emptyList()
        }

        return Lesson(
            id = id,
            dateMillis = dateMillis,
            numInDay = numInDay,
            subjectName = subjectName,
            subjectShort = subjectShort,
            topic = topic,
            teacherName = teacherName,
            homeworkText = homeworkText,
            homeworkFiles = files,
            isHomeworkDone = isDone,
            gradeValue = gradeValue,
            gradeWeight = gradeWeight
        )
    }

    private fun GradeEntity.toDomain(): Grade {
        return Grade(
            id = id.toString(),
            lessonId = lessonId,
            subjectName = subject,
            value = markVal,
            numericValue = markNum,
            weight = weight,
            date = markDate,
            typeName = workType,
            teacherName = teacherFio
        )
    }
}
