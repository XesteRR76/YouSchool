package org.youschool.app.domain.model

data class Lesson(
    val id: Long,
    val dateMillis: Long,
    val numInDay: Int,
    val subjectName: String,
    val subjectShort: String,
    val topic: String,
    val teacherName: String,
    val homeworkText: String,
    val homeworkFiles: List<HomeworkAttachment>,
    val isHomeworkDone: Boolean = false,
    val gradeValue: String? = null,
    val gradeWeight: Int? = null
)

data class HomeworkAttachment(
    val fileId: Long,
    val fileName: String
)

data class Grade(
    val id: String,
    val lessonId: Long?,
    val subjectName: String,
    val value: String,
    val numericValue: Double,
    val weight: Int,
    val date: String,
    val typeName: String,
    val teacherName: String
)

data class SubjectAverage(
    val subjectName: String,
    val averageScore: Double,
    val gradesCount: Int,
    val targetFiveNeeded: Int = 0
)

data class UserProfile(
    val userId: Long,
    val prsId: Long,
    val fullName: String,
    val schoolName: String,
    val role: String
)
