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
    val gradeWeight: Int?
)

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lessonId: Long?,
    val unitId: Long?,
    val subject: String,
    val weight: Int,
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
