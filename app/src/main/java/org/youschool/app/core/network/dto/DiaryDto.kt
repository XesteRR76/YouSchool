package org.youschool.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiaryResponse(
    val lesson: List<LessonDto> = emptyList()
)

@Serializable
data class LessonDto(
    val id: Long,
    val date: String,
    val numInDay: Int = 1,
    val isODOD: Int = 0,
    val unit: UnitDto? = null,
    val tp: TopicDto? = null,
    val teacher: TeacherDto? = null,
    val part: List<PartDto> = emptyList()
)

@Serializable
data class UnitDto(
    val id: Long? = null,
    val name: String? = null,
    val short: String? = null
)

@Serializable
data class TopicDto(
    val topicName: String? = null
)

@Serializable
data class TeacherDto(
    val factTeacherIN: String? = null
)

@Serializable
data class PartDto(
    val cat: String? = null,
    val variant: List<VariantDto> = emptyList()
)

@Serializable
data class VariantDto(
    val text: String? = null,
    val file: List<FileDto> = emptyList()
)

@Serializable
data class FileDto(
    val id: Long,
    val fileName: String
)
