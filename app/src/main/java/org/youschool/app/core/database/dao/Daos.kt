package org.youschool.app.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.youschool.app.core.database.entities.GradeEntity
import org.youschool.app.core.database.entities.HomeworkStatusEntity
import org.youschool.app.core.database.entities.LessonEntity
import org.youschool.app.core.database.entities.PendingHomeworkSyncEntity
import org.youschool.app.core.database.entities.ReadGradeEntity

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay ORDER BY numInDay ASC")
    fun getLessonsForDay(startOfDay: Long, endOfDay: Long): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    suspend fun getLessonById(lessonId: Long): LessonEntity?

    @Query("SELECT * FROM lessons WHERE homeworkText != '' ORDER BY dateMillis DESC")
    fun getAllLessonsWithHomework(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE (homeworkText != '' OR homeworkFilesJson != '[]') AND dateMillis >= :todayStartMillis ORDER BY dateMillis ASC, numInDay ASC")
    fun getUpcomingHomeworks(todayStartMillis: Long): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Query("DELETE FROM lessons WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay")
    suspend fun deleteLessonsForDay(startOfDay: Long, endOfDay: Long)

    @Transaction
    suspend fun replaceLessonsForRange(startOfDay: Long, endOfDay: Long, lessons: List<LessonEntity>) {
        deleteLessonsForDay(startOfDay, endOfDay)
        insertLessons(lessons)
    }
}

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades ORDER BY id DESC")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades")
    suspend fun getAllGradesSnapshot(): List<GradeEntity>

    @Query("SELECT * FROM grades WHERE subject = :subjectName ORDER BY id DESC")
    fun getGradesForSubject(subjectName: String): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    @Query("DELETE FROM grades")
    suspend fun clearAllGrades()

    @Transaction
    suspend fun replaceAllGrades(grades: List<GradeEntity>) {
        clearAllGrades()
        insertGrades(grades)
    }
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

@Dao
interface ReadGradeDao {
    @Query("SELECT * FROM read_grades")
    fun getAllReadGrades(): Flow<List<ReadGradeEntity>>

    @Query("SELECT markKey FROM read_grades")
    suspend fun getAllReadGradeKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markAsRead(entities: List<ReadGradeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markAsRead(entity: ReadGradeEntity)

    @Query("DELETE FROM read_grades WHERE markKey = :markKey")
    suspend fun unmarkAsRead(markKey: String)

    @Query("DELETE FROM read_grades WHERE markKey IN (:markKeys)")
    suspend fun unmarkMultipleAsRead(markKeys: List<String>)
}

@Dao
interface PendingHomeworkSyncDao {
    @Query("SELECT * FROM pending_homework_sync ORDER BY timestamp ASC")
    fun getAllPending(): Flow<List<PendingHomeworkSyncEntity>>

    @Query("SELECT * FROM pending_homework_sync ORDER BY timestamp ASC")
    suspend fun getAllPendingList(): List<PendingHomeworkSyncEntity>

    @Query("SELECT * FROM pending_homework_sync WHERE partId = :partId")
    suspend fun getPendingByPartId(partId: Long): PendingHomeworkSyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entity: PendingHomeworkSyncEntity)

    @Query("DELETE FROM pending_homework_sync WHERE partId = :partId")
    suspend fun remove(partId: Long)
}
