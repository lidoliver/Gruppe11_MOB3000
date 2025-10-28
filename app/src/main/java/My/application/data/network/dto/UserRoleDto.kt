package my.application.data.network.dto

import com.squareup.moshi.Json

data class UserRoleDto(
    @Json(name = "role") val role: String
)
