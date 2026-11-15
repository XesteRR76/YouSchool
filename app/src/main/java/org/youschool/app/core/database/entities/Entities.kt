package org.youschool.app.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey
    val id: Long,
    val dateMillis: Long,
    val numInDay: Int,
    val subjectId: Long?,
    val subjectName: String,
    val subjectShort: String,
    val topic: String,
    val teacherName: String,
    val homeworkText: String,
    val homeworkFilesJson: String,
    val gradeValue: String?,
    val gradeWeight: Int?,
    val homeworkPartId: Long? = null
)

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lessonId: Long?,
    val unitId: Long?,
    val subject: String,
    val weight: Double,
    val markVal: String,
    val markNum: Double,
    val markDate: String,
    val workType: String,
    val teacherFio: String
)

@Entity(tableName = "homework_status")
data class HomeworkStatusEntity(
    @PrimaryKey
    val lessonId: Long,
    val isCompleted: Boolean,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "read_grades")
data class ReadGradeEntity(
    @PrimaryKey
    val markKey: String,
    val readTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pending_homework_sync")
data class PendingHomeworkSyncEntity(
    @PrimaryKey
    val partId: Long,
    val lessonId: Long,
    val isCompleted: Boolean,
    val comment: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
