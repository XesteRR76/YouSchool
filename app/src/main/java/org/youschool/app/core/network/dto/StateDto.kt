package org.youschool.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class StateResponse(
    val userId: Long? = null,
    val authenticated: Boolean = false,
    val profile: ProfileDto? = null,
    val user: UserDto? = null
)

@Serializable
data class ProfileDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val email: String? = null,
    val phoneMob: String? = null
)

@Serializable
data class UserDto(
    val userId: Long? = null,
    val prsId: Long? = null,
    val roles: List<String> = emptyList(),
    val currentPosition: PositionDto? = null
)

@Serializable
data class PositionDto(
    val orgId: Long? = null,
    val orgName: String? = null,
    val orgFullName: String? = null,
    val orgYearId: Long? = null
)

@Serializable
data class ChildDto(
    val userId: Long,
    val prsId: Long? = null,
    val fio: String? = null
)
