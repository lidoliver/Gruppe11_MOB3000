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

// DTO for admin panel brukere (basert på backend struktur)
data class AdminUserDto(
    @Json(name = "email") val email: String,
    @Json(name = "brukernavn") val username: String?,
    @Json(name = "role") val role: String?,
    @Json(name = "club_id") val clubId: Int?,
    @Json(name = "club_name") val clubName: String?
) {
    // Convert to UserDto for compatibility
    fun toUserDto(): UserDto = UserDto(
        email = email,
        username = username,
        firstName = null,
        lastName = null,
        role = role,
        clubId = clubId,
        clubName = clubName
    )
}

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
    @Json(name = "pointcard_id") val id: Int,
    @Json(name = "spiller_navn") val userEmail: String,
    @Json(name = "bane_id") val courseId: Int?,
    @Json(name = "dato") val date: String?,
    @Json(name = "total_score") val totalScore: Int,
    @Json(name = "score") val score: Any? = null, // JSONB field
    @Json(name = "oppdatert_dato") val oppdatert_dato: String? = null
)

data class ChangePasswordRequest(
    @Json(name = "currentPassword") val currentPassword: String,
    @Json(name = "newPassword") val newPassword: String
)

data class ChangePasswordResponse(
    @Json(name = "message") val message: String
)

data class UsernameAvailabilityResponse(
    @Json(name = "available") val available: Boolean,
    @Json(name = "message") val message: String
)

// Admin-DTOs
data class ClubRequestDto(
    @Json(name = "id") val id: Int,
    @Json(name = "requested_by") val requestedBy: String,
    @Json(name = "klubbnavn") val clubName: String,
    @Json(name = "sted") val location: String?,
    @Json(name = "beskrivelse") val description: String?,
    @Json(name = "status") val status: String,
    @Json(name = "request_date") val createdAt: String
)

data class SubmitClubRequestDto(
    @Json(name = "klubbnavn") val clubName: String,
    @Json(name = "beskrivelse") val description: String
)

data class ClubRequestResponseDto(
    @Json(name = "message") val message: String,
    @Json(name = "requestId") val requestId: Int?
)

data class AdminResponseDto(
    @Json(name = "message") val message: String
)

data class ClubDto(
    @Json(name = "id_klubb") val id: Int,
    @Json(name = "klubbnavn") val name: String,
    @Json(name = "sted") val location: String?,
    @Json(name = "klubbeskrivelse") val description: String?,
    @Json(name = "kontakt_email") val contactEmail: String?,
    @Json(name = "kontakt_telefon") val contactPhone: String?,
    @Json(name = "member_count") val memberCount: Int?
)

data class SubmitClubJoinRequestDto(
    @Json(name = "klubId") val clubId: Int,
    @Json(name = "message") val message: String?
)

data class ClubJoinRequestResponseDto(
    @Json(name = "message") val message: String,
    @Json(name = "requestId") val requestId: Int?
)

data class ClubJoinRequestDto(
    @Json(name = "id") val id: Int,
    @Json(name = "user_email") val userEmail: String,
    @Json(name = "klub_id") val clubId: Int,
    @Json(name = "message") val message: String?,
    @Json(name = "status") val status: String,
    @Json(name = "request_date") val requestDate: String,
    @Json(name = "brukernavn") val username: String?,
    @Json(name = "klubbnavn") val clubName: String?
)

