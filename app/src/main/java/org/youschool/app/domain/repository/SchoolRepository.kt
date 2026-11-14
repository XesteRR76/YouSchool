package org.youschool.app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.youschool.app.domain.model.Grade
import org.youschool.app.domain.model.Lesson
import org.youschool.app.domain.model.SubjectAverage
import org.youschool.app.domain.model.UserProfile

interface SchoolRepository {
    fun getLessonsForDay(dateMillis: Long): Flow<List<Lesson>>
    fun getAllHomeworks(): Flow<List<Lesson>>
    fun getAllGrades(): Flow<List<Grade>>
    fun getSubjectAverages(): Flow<List<SubjectAverage>>
    
    suspend fun syncDayLessons(dateMillis: Long): Result<Unit>
    suspend fun syncGrades(): Result<Unit>
    suspend fun toggleHomeworkStatus(lessonId: Long, isDone: Boolean)
    
    suspend fun login(login: String, rawPassword: String): Result<UserProfile>
    fun isUserLoggedIn(): Boolean
    fun getUserProfile(): UserProfile?
    fun logout()
}
