package my.application.data.remote.dto

import com.squareup.moshi.Json

data class UserDto(
    @Json(name = "epost") val email: String,
    @Json(name = "brukernavn") val username: String?,
    @Json(name = "fnavn") val firstName: String?,
    @Json(name = "enavn") val lastName: String?,
    @Json(name = "role") val role: String?,
    @Json(name = "klubb_id_klubb") val clubId: Int?,
    @Json(name = "klubbnavn") val clubName: String?
)

data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

data class LoginResponse(
    @Json(name = "token") val token: String,
    @Json(name = "email") val email: String
)

data class RegisterRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

data class RegisterResponse(
    @Json(name = "message") val message: String
)

data class ScorecardDto(
    @Json(name = "id_poengkort") val id: Int,
    @Json(name = "epost_bruker") val userEmail: String,
    @Json(name = "bane_id_bane") val courseId: Int?,
    @Json(name = "dato") val date: String,
    @Json(name = "totalt_poeng") val totalScore: Int
)

