package my.application.data.network.dto

import com.squareup.moshi.Json

data class UserDto(
    @Json(name = "epost") val email: String,
    @Json(name = "navn") val name: String,
    @Json(name = "profilbilde") val profilePicture: String?,
    @Json(name = "role") val role: String
)
