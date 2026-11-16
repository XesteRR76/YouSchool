package org.youschool.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.youschool.app.core.notification.NotificationHelper
import org.youschool.app.core.notification.NotificationRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.youschool.app.core.auth.AuthManager
import org.youschool.app.core.database.YouSchoolDatabase
import org.youschool.app.core.database.entities.GradeEntity
import org.youschool.app.core.database.entities.HomeworkStatusEntity
import org.youschool.app.core.database.entities.LessonEntity
import org.youschool.app.core.database.entities.PendingHomeworkSyncEntity
import org.youschool.app.core.database.entities.PendingHomeworkUploadEntity
import org.youschool.app.core.database.entities.ReadGradeEntity
import org.youschool.app.core.network.EschoolApiClient
import org.youschool.app.core.network.dto.FileDto
import org.youschool.app.domain.model.*
import org.youschool.app.domain.repository.SchoolRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SchoolRepositoryImpl(
    private val context: Context,
    private val database: YouSchoolDatabase,
    private val apiClient: EschoolApiClient,
    private val authManager: AuthManager
) : SchoolRepository {

    private val syncMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getMessageThreads(before: Long?) = apiClient.getMessageThreads(before)
    override suspend fun getMessageThread(id: Long) = apiClient.getMessageThread(id)
    override suspend fun getMessages(id: Long, before: Long?) = apiClient.getMessages(id, before)
    override suspend fun getMessageRecipients() = apiClient.getMessageRecipients()
    override suspend fun getNoReplyId() = apiClient.getNoReplyId()
    override suspend fun createPrivateThread(recipientId: Long) = apiClient.createPrivateThread(recipientId)
    override suspend fun sendMessage(threadId: Long, text: String, uid: String) = apiClient.sendMessage(threadId, text, uid)
    override suspend fun markMessageRead(id: Long) = apiClient.markMessageRead(id)

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
        val gradesFlow = database.gradeDao().getAllGrades()

        return combine(lessonsFlow, statusFlow, gradesFlow) { entities, statuses, grades ->
            val statusMap = statuses.associateBy { it.lessonId }
            val gradesByLesson = grades.groupBy { it.lessonId }
            entities.map { entity ->
                val lessonGrades = (gradesByLesson[entity.id] ?: emptyList()).map { it.toDomain() }
                entity.toDomain(
                    isDone = statusMap[entity.id]?.isCompleted ?: false,
                    lessonGrades = lessonGrades
                )
            }
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

    override fun getUpcomingHomeworks(): Flow<List<Lesson>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis

        val lessonsFlow = database.lessonDao().getUpcomingHomeworks(startOfToday)
        val statusFlow = database.homeworkStatusDao().getAllStatuses()

        return combine(lessonsFlow, statusFlow) { entities, statuses ->
            val statusMap = statuses.associateBy { it.lessonId }
            entities.map { it.toDomain(statusMap[it.id]?.isCompleted ?: false) }
        }
    }

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun getNewGrades(): Flow<List<Grade>> {
        val gradesFlow = database.gradeDao().getAllGrades()
        val readFlow = database.readGradeDao().getAllReadGrades()
        val prefs = context.getSharedPreferences("youschool_prefs", Context.MODE_PRIVATE)

        return combine(gradesFlow, readFlow) { grades, readEntities ->
            val userId = authManager.getUserId()
            val initializedKey = "initialized_grades_v2_$userId"
            if (!prefs.getBoolean(initializedKey, false)) {
                // Initial sync has not yet completed; ignore past grades
                return@combine emptyList()
            }

            val readKeySet = readEntities.map { it.markKey }.toSet()
            val sevenDaysAgoMs = System.currentTimeMillis() - 7 * 86400000L

            grades.mapNotNull { entity ->
                val key = "${entity.lessonId ?: 0}_${entity.markDate}_${entity.markVal}"
                if (readKeySet.contains(key)) return@mapNotNull null

                val timestamp = try {
                    isoDateFormat.parse(entity.markDate)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }

                // Threshold: show marks added within the last 7 days
                if (timestamp < sevenDaysAgoMs && timestamp > 0L) {
                    return@mapNotNull null
                }

                entity.toDomain(isRead = false)
            }.sortedByDescending { it.date }
        }
    }

    override fun getAllGrades(): Flow<List<Grade>> {
        val gradesFlow = database.gradeDao().getAllGrades()
        val readFlow = database.readGradeDao().getAllReadGrades()

        return combine(gradesFlow, readFlow) { grades, readEntities ->
            val readKeySet = readEntities.map { it.markKey }.toSet()
            grades.map { entity ->
                val key = "${entity.lessonId ?: 0}_${entity.markDate}_${entity.markVal}"
                entity.toDomain(isRead = readKeySet.contains(key))
            }
        }
    }

    override fun getSubjectAverages(): Flow<List<SubjectAverage>> {
        return database.gradeDao().getAllGrades().map { entities ->
            val grouped = entities.groupBy { it.subject }
            grouped.map { (subject, grades) ->
                var totalWeightedScore = 0.0
                var totalWeight = 0.0
                for (g in grades) {
                    if (g.markNum > 0) {
                        totalWeightedScore += g.markNum * g.weight
                        totalWeight += g.weight
                    }
                }
                val avg = if (totalWeight > 0) totalWeightedScore / totalWeight else 0.0

                SubjectAverage(
                    subjectName = subject,
                    averageScore = avg,
                    gradesCount = grades.size,
                    totalWeight = totalWeight
                )
            }.sortedBy { it.subjectName }
        }
    }

    override suspend fun syncDayLessons(dateMillis: Long): Result<Unit> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val userId = authManager.getUserId()
            if (userId <= 0) return@withContext Result.failure(Exception("Пользователь не авторизован"))

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
            val syncStart = startOfDay - 7 * 86400000L
            val syncEnd = endOfDay + 7 * 86400000L

            try {
                val response = apiClient.getDiary(userId, syncStart, syncEnd)
                if (response.isSuccess) {
                    if (authManager.getUserId() != userId) return@withContext Result.failure(Exception("Аккаунт изменился"))
                    val lessonsDto = response.getOrNull()?.lesson ?: emptyList()
                    val entities = lessonsDto.map { dto ->
                        var homeworkText = ""
                        val filesList = mutableListOf<FileDto>()
                        var homeworkPartId: Long? = null
                        var isServerDone: Boolean? = null

                        for (part in dto.part) {
                            if (part.cat == "DZ") {
                                homeworkPartId = part.id
                                if (part.ready != null) {
                                    isServerDone = (part.ready == 1)
                                }
                                for (v in part.variant) {
                                    if (!v.text.isNullOrBlank()) {
                                        val cleanText = android.text.Html.fromHtml(v.text, android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim()
                                        homeworkText += (if (homeworkText.isNotEmpty()) "\n" else "") + cleanText
                                    }
                                    for (f in v.file) {
                                        filesList.add(f.copy(variantId = v.id))
                                    }
                                }
                            }
                        }

                        // Two-way sync: if server has a status for this homework and local user hasn't queued an offline change, reflect server status
                        if (homeworkPartId != null && isServerDone != null) {
                            val pending = database.pendingHomeworkSyncDao().getPendingByPartId(homeworkPartId)
                            if (pending == null) {
                                database.homeworkStatusDao().setStatus(
                                    HomeworkStatusEntity(dto.id, isServerDone)
                                )
                            }
                        }

                        val timeRange = org.youschool.app.ui.util.LessonTimeFormatter.formatLessonTimeRange(
                            startRaw = dto.start ?: dto.startTime ?: dto.startDt,
                            endRaw = dto.end ?: dto.endTime ?: dto.endDt,
                            timeRaw = dto.time,
                            bellRaw = dto.bell
                        )

                        LessonEntity(
                            id = dto.id,
                            dateMillis = dto.date ?: startOfDay,
                            numInDay = dto.numInDay,
                            subjectId = dto.unit?.id,
                            subjectName = dto.unit?.name ?: "Урок",
                            subjectShort = dto.unit?.short ?: dto.unit?.name ?: "Урок",
                            topic = dto.tp?.topicName ?: "",
                            teacherName = dto.teacher?.factTeacherIN ?: "",
                            homeworkText = homeworkText,
                            homeworkFilesJson = json.encodeToString(filesList),
                            gradeValue = null,
                            gradeWeight = null,
                            homeworkPartId = homeworkPartId,
                            timeRange = timeRange
                        )
                    }

                    database.lessonDao().replaceLessonsForRange(syncStart, syncEnd, entities)
                    Result.success(Unit)
                } else {
                    Result.failure(response.exceptionOrNull() ?: Exception("Ошибка загрузки расписания"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun syncGrades(): Result<Unit> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val userId = authManager.getUserId()
            if (userId <= 0) return@withContext Result.failure(Exception("Пользователь не авторизован"))

            try {
                val defaultPeriodResult = apiClient.getDefaultPeriodId(userId)
                val currentPeriodId = defaultPeriodResult.getOrElse { return@withContext Result.failure(it) }

                // Fetch units to get accurate subject names
                val unitsResult = apiClient.getDiaryUnits(userId, currentPeriodId)
                val unitNames = unitsResult.getOrNull()?.result?.associate { (it.unitId ?: 0L) to (it.unitName ?: "") } ?: emptyMap()

                val gradesResult = apiClient.getGradesForPeriod(userId, currentPeriodId)
                if (gradesResult.isSuccess) {
                    val gradesDto = gradesResult.getOrNull()?.result ?: emptyList()
                    val entities = gradesDto.filter { !it.markVal.isNullOrBlank() }.map { dto ->
                        val actualMark = dto.markVal?.toDoubleOrNull() ?: 0.0
                        val subjectName = unitNames[dto.unitId] ?: "Предмет"

                        GradeEntity(
                            lessonId = dto.lessonId ?: 0L,
                            unitId = dto.unitId,
                            subject = subjectName,
                            weight = dto.mktWt ?: 1.0,
                            markVal = dto.markVal ?: "",
                            markNum = actualMark,
                            markDate = dto.markDate ?: "",
                            lessonDate = dto.startDt ?: "",
                            workType = dto.lptName ?: "",
                            teacherFio = dto.teachFio ?: ""
                        )
                    }
                    val prefs = context.getSharedPreferences("youschool_prefs", Context.MODE_PRIVATE)
                    if (authManager.getUserId() != userId) return@withContext Result.failure(Exception("Аккаунт изменился"))
                    val initializedKey = "initialized_grades_v2_$userId"
                    val notificationPrefs = context.getSharedPreferences("youschool_notification_state", Context.MODE_PRIVATE)
                    val seenKey = "grades_${userId}_$currentPeriodId"
                    val keys = entities.associateBy { "${it.unitId}_${it.lessonId}_${it.markDate}_${it.markVal}_${it.workType}" }
                    val seen = notificationPrefs.getStringSet(seenKey, null)?.toSet()
                    val isFirstGradesSync = !prefs.getBoolean(initializedKey, false) || seen == null
                    val newKeys = NotificationRules.newKeys(keys.keys, seen)

                    if (isFirstGradesSync) {
                        // On first launch / initial sync, mark all existing past grades as already read
                        // so that only genuinely new grades added in subsequent syncs will be shown in "Новые оценки"
                        val readEntities = entities.map { entity ->
                            val key = "${entity.lessonId ?: 0}_${entity.markDate}_${entity.markVal}"
                            ReadGradeEntity(key)
                        }
                        database.readGradeDao().markAsRead(readEntities)
                        prefs.edit().putBoolean(initializedKey, true).apply()
                    } else if (newKeys.isNotEmpty()) {
                        // In subsequent syncs, guarantee all previous (non-new) grades are marked as read
                        val nonNewEntities = entities.filter { entity ->
                            val key = "${entity.unitId}_${entity.lessonId}_${entity.markDate}_${entity.markVal}_${entity.workType}"
                            !newKeys.contains(key)
                        }.map { entity ->
                            val key = "${entity.lessonId ?: 0}_${entity.markDate}_${entity.markVal}"
                            ReadGradeEntity(key)
                        }
                        if (nonNewEntities.isNotEmpty()) {
                            database.readGradeDao().markAsRead(nonNewEntities)
                        }
                    }

                    database.gradeDao().replaceAllGrades(entities)

                    notificationPrefs.edit().putStringSet(seenKey, (seen.orEmpty() + keys.keys).toSet()).apply()
                    val settings = (context.applicationContext as org.youschool.app.YouSchoolApp).themeManager.themeSettings.value
                    if (settings.areNotificationsEnabled && settings.gradeNotificationsEnabled && newKeys.isNotEmpty()) {
                        NotificationHelper.showNewGradesNotification(context, newKeys.mapNotNull { keys[it] })
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(gradesResult.exceptionOrNull() ?: Exception("Ошибка загрузки оценок"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            return false
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            val hasPhysical = cm.allNetworks.any { net ->
                if (net == active) return@any false
                val c = cm.getNetworkCapabilities(net) ?: return@any false
                !c.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                        c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
            if (!hasPhysical) return false
        }
        return true
    }

    override suspend fun toggleHomeworkStatus(lessonId: Long, isDone: Boolean, comment: String?) = withContext(Dispatchers.IO) {
        // 1. Instant local update for offline-first responsiveness
        database.homeworkStatusDao().setStatus(HomeworkStatusEntity(lessonId, isDone))

        // 2. Synchronize with server if lesson has a homework partId
        val lesson = database.lessonDao().getLessonById(lessonId)
        val partId = lesson?.homeworkPartId
        android.util.Log.d("SchoolRepository", "toggleHomeworkStatus: lessonId=$lessonId, partId=$partId, isNetworkAvailable=${isNetworkAvailable()}")
        if (partId != null) {
            if (!isNetworkAvailable()) {
                android.util.Log.d("SchoolRepository", "Enqueuing offline homework sync: partId=$partId, isDone=$isDone")
                database.pendingHomeworkSyncDao().enqueue(
                    PendingHomeworkSyncEntity(partId, lessonId, isDone, comment)
                )
            } else {
                val result = apiClient.setHomeworkDone(partId, isDone, needRecheck = true, comment = comment)
                if (result.isSuccess) {
                    database.pendingHomeworkSyncDao().remove(partId)
                } else {
                    android.util.Log.d("SchoolRepository", "API failed, enqueuing offline sync: partId=$partId")
                    database.pendingHomeworkSyncDao().enqueue(
                        PendingHomeworkSyncEntity(partId, lessonId, isDone, comment)
                    )
                }
            }
        }
    }

    override suspend fun syncPendingHomeworkStatus(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pending = database.pendingHomeworkSyncDao().getAllPendingList()
            for (item in pending) {
                val result = apiClient.setHomeworkDone(item.partId, item.isCompleted, needRecheck = true, comment = item.comment)
                if (result.isSuccess) {
                    database.pendingHomeworkSyncDao().remove(item.partId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enqueueHomeworkResultUpload(partId: Long, file: File): Result<PendingHomeworkUploadEntity> = withContext(Dispatchers.IO) {
        try {
            val uploadsDir = File(context.filesDir, "pending_uploads").apply { mkdirs() }
            val persistentFile = File(uploadsDir, "upload_${System.currentTimeMillis()}_${file.name}")
            file.copyTo(persistentFile, overwrite = true)
            val entity = PendingHomeworkUploadEntity(
                partId = partId,
                localFilePath = persistentFile.absolutePath,
                fileName = file.name,
                fileSize = persistentFile.length(),
                fileType = null
            )
            val id = database.pendingHomeworkUploadDao().enqueue(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPendingHomeworkUploads(partId: Long): Flow<List<PendingHomeworkUploadEntity>> {
        return database.pendingHomeworkUploadDao().getPendingForPartFlow(partId)
    }

    override suspend fun syncPendingHomeworkUploads(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pending = database.pendingHomeworkUploadDao().getAllPendingList()
            var failure: Exception? = null
            for (item in pending) {
                val file = File(item.localFilePath)
                if (!file.exists()) {
                    database.pendingHomeworkUploadDao().remove(item.id)
                    continue
                }
                val uploadResult = uploadHomeworkResult(item.partId, file)
                if (uploadResult.isSuccess) {
                    database.pendingHomeworkUploadDao().remove(item.id)
                    try { file.delete() } catch (_: Exception) {}
                } else {
                    failure = uploadResult.exceptionOrNull() as? Exception ?: Exception("Upload failed")
                }
            }
            if (failure != null) Result.failure(failure) else Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removePendingHomeworkUpload(uploadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pending = database.pendingHomeworkUploadDao().getAllPendingList().find { it.id == uploadId }
            if (pending != null) {
                try { File(pending.localFilePath).delete() } catch (_: Exception) {}
            }
            database.pendingHomeworkUploadDao().remove(uploadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPendingSyncCount(): Flow<Int> {
        return combine(
            database.pendingHomeworkSyncDao().getPendingCountFlow(),
            database.pendingHomeworkUploadDao().getPendingCountFlow()
        ) { a, b -> a + b }
    }

    override suspend fun syncAllPending(): Result<Unit> = withContext(Dispatchers.IO) {
        var err: Throwable? = null
        val statusRes = syncPendingHomeworkStatus()
        if (statusRes.isFailure) err = statusRes.exceptionOrNull()
        val uploadRes = syncPendingHomeworkUploads()
        if (uploadRes.isFailure && err == null) err = uploadRes.exceptionOrNull()
        if (err != null) Result.failure(err) else Result.success(Unit)
    }

    override suspend fun getHomeworkResultFiles(partId: Long): Result<List<HomeworkResultFile>> = withContext(Dispatchers.IO) {
        val prsId = authManager.getPrsId()
        val detailsResult = apiClient.getHomeworkDetails(partId, prsId)
        detailsResult.map { details ->
            details.attach.filter { it.is_result == 1 }.map { fileDto ->
                HomeworkResultFile(
                    fileId = fileDto.fileId,
                    fileName = fileDto.fileName,
                    fileSize = fileDto.fileSize,
                    fileType = fileDto.fileType
                )
            }
        }
    }

    override suspend fun uploadHomeworkResult(partId: Long, file: File): Result<HomeworkResultFile> = withContext(Dispatchers.IO) {
        val prsId = authManager.getPrsId()
        val uploadResult = apiClient.uploadHomeworkResultFile(partId, prsId, file)
        uploadResult.map { dto ->
            HomeworkResultFile(
                fileId = dto.fileId,
                fileName = dto.fileName ?: file.name,
                fileSize = dto.fileSize ?: file.length(),
                fileType = dto.contentType
            )
        }
    }

    override suspend fun deleteHomeworkResult(partId: Long, fileId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val prsId = authManager.getPrsId()
        val delResult = apiClient.deleteHomeworkResultFile(partId, prsId, fileId)
        delResult.map { Unit }
    }

    override suspend fun login(login: String, rawPassword: String, captchaToken: String?): Result<UserProfile> {
        val hash = AuthManager.sha256(rawPassword)
        val result = apiClient.login(login, hash, captchaToken)
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

            org.youschool.app.core.notification.GradesNotificationScheduler.schedule(context)
            val profile = UserProfile(userId, prsId, fullName, school, role)
            Result.success(profile)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Ошибка авторизации"))
        }
    }

    override suspend fun loginDemo(): Result<UserProfile> {
        authManager.saveCredentials("demo", "demo")
        authManager.saveUserInfo(1001L, 2002L, "Иван Иванов", "Гимназия №1")
        val profile = UserProfile(1001L, 2002L, "Иван Иванов", "Гимназия №1", "ROLE_STUDENT")
        return Result.success(profile)
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
        val userId = authManager.getUserId()
        authManager.clear()
        NotificationHelper.cancelAll(context)
        org.youschool.app.core.notification.GradesNotificationScheduler.cancel(context)
        context.getSharedPreferences("youschool_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("initialized_grades_$userId")
            .remove("initialized_grades_v2_$userId")
            .apply()
    }

    override suspend fun markGradeAsRead(markKey: String) {
        withContext(Dispatchers.IO) {
            database.readGradeDao().markAsRead(ReadGradeEntity(markKey))
        }
    }

    override suspend fun markAllGradesAsRead() {
        withContext(Dispatchers.IO) {
            val grades = database.gradeDao().getAllGrades().firstOrNull() ?: emptyList()
            val entities = grades.map { entity ->
                val key = "${entity.lessonId ?: 0}_${entity.markDate}_${entity.markVal}"
                ReadGradeEntity(key)
            }
            database.readGradeDao().markAsRead(entities)
        }
    }

    override suspend fun restoreGradeReadStatus(markKeys: List<String>) {
        withContext(Dispatchers.IO) {
            database.readGradeDao().unmarkMultipleAsRead(markKeys)
        }
    }

    override suspend fun unmarkAllGradesAsRead() {
        withContext(Dispatchers.IO) {
            val keys = database.readGradeDao().getAllReadGradeKeys()
            database.readGradeDao().unmarkMultipleAsRead(keys)
        }
    }

    override suspend fun downloadHomeworkFile(fileId: Long, fileName: String, variantId: Long?): Result<File> = withContext(Dispatchers.IO) {
        val safeName = fileName.replace(Regex("[/\\\\?%*:|\"<>]"), "_")
        val destinationFile = File(context.cacheDir, "homework_files/$fileId/$safeName")
        if (destinationFile.exists() && destinationFile.length() > 0) {
            return@withContext Result.success(destinationFile)
        }

        var actualVariantId = variantId
        if (actualVariantId == null) {
            syncDayLessons(System.currentTimeMillis())
            val lessons = database.lessonDao().getAllLessonsWithHomework().firstOrNull() ?: emptyList()
            for (lesson in lessons) {
                try {
                    val dtos = json.decodeFromString<List<FileDto>>(lesson.homeworkFilesJson)
                    val match = dtos.find { it.id == fileId }
                    if (match?.variantId != null) {
                        actualVariantId = match.variantId
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        apiClient.downloadFile(fileId, destinationFile, actualVariantId)
    }

    private fun LessonEntity.toDomain(
        isDone: Boolean,
        lessonGrades: List<Grade> = emptyList()
    ): Lesson {
        val files: List<HomeworkAttachment> = try {
            val dtos = json.decodeFromString<List<FileDto>>(homeworkFilesJson)
            dtos.mapNotNull { dto ->
                if (dto.id != null && dto.fileName != null) {
                    HomeworkAttachment(dto.id, dto.fileName, dto.variantId)
                } else null
            }
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
            gradeWeight = gradeWeight,
            grades = lessonGrades,
            homeworkPartId = homeworkPartId,
            timeRange = timeRange
        )
    }

    private fun GradeEntity.toDomain(isRead: Boolean = false): Grade {
        val key = "${lessonId ?: 0}_${markDate}_${markVal}"
        return Grade(
            id = id.toString(),
            lessonId = lessonId,
            subjectName = subject,
            value = markVal,
            numericValue = markNum,
            weight = weight,
            date = markDate,
            lessonDate = lessonDate,
            typeName = workType,
            teacherName = teacherFio,
            markKey = key,
            isRead = isRead
        )
    }
}
