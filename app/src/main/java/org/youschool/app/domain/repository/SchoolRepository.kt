package org.youschool.app.domain.repository

import java.io.File
import kotlinx.coroutines.flow.Flow
import org.youschool.app.domain.model.Grade
import org.youschool.app.domain.model.HomeworkResultFile
import org.youschool.app.domain.model.Lesson
import org.youschool.app.domain.model.SubjectAverage
import org.youschool.app.domain.model.UserProfile

interface SchoolRepository {
    fun getLessonsForDay(dateMillis: Long): Flow<List<Lesson>>
    fun getAllHomeworks(): Flow<List<Lesson>>
    fun getUpcomingHomeworks(): Flow<List<Lesson>>
    fun getAllGrades(): Flow<List<Grade>>
    fun getNewGrades(): Flow<List<Grade>>
    fun getSubjectAverages(): Flow<List<SubjectAverage>>
    
    suspend fun markGradeAsRead(markKey: String)
    suspend fun markAllGradesAsRead()
    suspend fun restoreGradeReadStatus(markKeys: List<String>)
    suspend fun downloadHomeworkFile(fileId: Long, fileName: String, variantId: Long? = null): Result<File>

    suspend fun syncDayLessons(dateMillis: Long): Result<Unit>
    suspend fun syncGrades(): Result<Unit>
    suspend fun toggleHomeworkStatus(lessonId: Long, isDone: Boolean, comment: String? = null)
    suspend fun syncPendingHomeworkStatus(): Result<Unit>
    suspend fun getHomeworkResultFiles(partId: Long): Result<List<HomeworkResultFile>>
    suspend fun uploadHomeworkResult(partId: Long, file: File): Result<HomeworkResultFile>
    suspend fun deleteHomeworkResult(partId: Long, fileId: Long): Result<Unit>
    
    suspend fun login(login: String, rawPassword: String, captchaToken: String? = null): Result<UserProfile>
    fun isUserLoggedIn(): Boolean
    fun getUserProfile(): UserProfile?
    fun logout()
}
