package org.youschool.app.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.youschool.app.core.database.entities.GradeEntity
import org.youschool.app.core.database.entities.HomeworkStatusEntity
import org.youschool.app.core.database.entities.LessonEntity

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay ORDER BY numInDay ASC")
    fun getLessonsForDay(startOfDay: Long, endOfDay: Long): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE homeworkText != '' ORDER BY dateMillis DESC")
    fun getAllLessonsWithHomework(): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Query("DELETE FROM lessons WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay")
    suspend fun deleteLessonsForDay(startOfDay: Long, endOfDay: Long)
}

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades ORDER BY id DESC")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE subject = :subjectName ORDER BY id DESC")
    fun getGradesForSubject(subjectName: String): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    @Query("DELETE FROM grades")
    suspend fun clearAllGrades()
}

@Dao
interface HomeworkStatusDao {
    @Query("SELECT * FROM homework_status")
    fun getAllStatuses(): Flow<List<HomeworkStatusEntity>>

    @Query("SELECT * FROM homework_status WHERE lessonId = :lessonId")
    suspend fun getStatusForLesson(lessonId: Long): HomeworkStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setStatus(status: HomeworkStatusEntity)
}
