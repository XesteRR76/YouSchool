package org.youschool.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class GradesResponse(
    val result: List<GradeDto> = emptyList()
)

@Serializable
data class GradeDto(
    val lessonId: Long? = null,
    val unitId: Long? = null,
    val subject: String? = null,
    val mktWt: Int = 1,
    val markVal: String? = null,
    val markNum: Double? = null,
    val markDate: String? = null,
    val lptName: String? = null,
    val teachFio: String? = null
)

@Serializable
data class UnitsResponse(
    val result: List<DiaryUnitDto> = emptyList()
)

@Serializable
data class DiaryUnitDto(
    val unitId: Long,
    val unitName: String,
    val overMark: Double? = null,
    val rating: String? = null,
    val totalMark: String? = null
)

@Serializable
data class PeriodContainerDto(
    val typeId: Int? = null,
    val typeName: String? = null,
    val items: List<PeriodItemDto> = emptyList()
)

@Serializable
data class PeriodItemDto(
    val id: Long,
    val name: String,
    val date1: Long,
    val date2: Long,
    val typeCode: String,
    val num: Int = 1
)
