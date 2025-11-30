package my.application.data.remote.dto

import com.squareup.moshi.Json

/**
 * Data transfer object for brukerdata fra backend
 * Inneholder informasjon om bruker, rolle og klubbtilhørighet
 */
data class UserDto(
    @Json(name = "epost") val email: String,
    @Json(name = "brukernavn") val username: String?,
    @Json(name = "fnavn") val firstName: String?,
    @Json(name = "enavn") val lastName: String?,
    @Json(name = "role") val role: String?,
    @Json(name = "klubb_id_klubb") val clubId: Int?,
    @Json(name = "klubbnavn") val clubName: String?
)

/**
 * DTO for admin panel brukere (basert på backend struktur)
 * Brukes i admin-grensesnittet for brukerstyring
 */
data class AdminUserDto(
    @Json(name = "email") val email: String,
    @Json(name = "brukernavn") val username: String?,
    @Json(name = "role") val role: String?,
    @Json(name = "club_id") val clubId: Int?,
    @Json(name = "club_name") val clubName: String?
) {
    /** Konverterer til UserDto for kompatibilitet med eksisterende kode */
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

/** Request body for innlogging */
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

/** Response fra innlogging med JWT-token */
data class LoginResponse(
    @Json(name = "token") val token: String,
    @Json(name = "email") val email: String
)

/** Request body for registrering av ny bruker */
data class RegisterRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

/** Response fra registrering */
data class RegisterResponse(
    @Json(name = "message") val message: String
)

/**
 * Data transfer object for spillkort fra backend
 * Inneholder poengsummer og metadata for en spillrunde
 */
data class ScorecardDto(
    @Json(name = "pointcard_id") val id: Int,
    @Json(name = "spiller_navn") val userEmail: String,
    @Json(name = "bane_id") val courseId: Int?,
    @Json(name = "dato") val date: String?,
    @Json(name = "total_score") val totalScore: Int,
    @Json(name = "score") val score: Any? = null, // JSONB-felt fra database
    @Json(name = "oppdatert_dato") val oppdatert_dato: String? = null
)

/** Request for endring av passord */
data class ChangePasswordRequest(
    @Json(name = "currentPassword") val currentPassword: String,
    @Json(name = "newPassword") val newPassword: String
)

/** Response fra passordendring */
data class ChangePasswordResponse(
    @Json(name = "message") val message: String
)

/** Response for sjekk av brukernavn-tilgjengelighet */
data class UsernameAvailabilityResponse(
    @Json(name = "available") val available: Boolean,
    @Json(name = "message") val message: String
)

// Admin-DTOs
/**
 * Data transfer object for klubbforespørsel
 * Brukes i admin-panelet for godkjenning/avvisning av nye klubber
 */
data class ClubRequestDto(
    @Json(name = "id") val id: Int,
    @Json(name = "requested_by") val requestedBy: String,
    @Json(name = "klubbnavn") val clubName: String,
    @Json(name = "sted") val location: String?,
    @Json(name = "beskrivelse") val description: String?,
    @Json(name = "status") val status: String,
    @Json(name = "request_date") val createdAt: String
)

/** Request body for innsending av klubbforespørsel */
data class SubmitClubRequestDto(
    @Json(name = "klubbnavn") val clubName: String,
    @Json(name = "beskrivelse") val description: String
)

/** Response fra innsending av klubbforespørsel */
data class ClubRequestResponseDto(
    @Json(name = "message") val message: String,
    @Json(name = "requestId") val requestId: Int?
)

/** Generisk admin-response */
data class AdminResponseDto(
    @Json(name = "message") val message: String
)

/**
 * Data transfer object for klubbdata
 * Inneholder all informasjon om en disc golf-klubb
 */
data class ClubDto(
    @Json(name = "id_klubb") val id: Int,
    @Json(name = "klubbnavn") val name: String,
    @Json(name = "sted") val location: String?,
    @Json(name = "klubbeskrivelse") val description: String?,
    @Json(name = "kontakt_email") val contactEmail: String?,
    @Json(name = "kontakt_telefon") val contactPhone: String?,
    @Json(name = "member_count") val memberCount: Int?
)

/** Request body for medlemsforespørsel til klubb */
data class SubmitClubJoinRequestDto(
    @Json(name = "klubId") val clubId: Int,
    @Json(name = "message") val message: String?
)

/** Response fra medlemsforespørsel */
data class ClubJoinRequestResponseDto(
    @Json(name = "message") val message: String,
    @Json(name = "requestId") val requestId: Int?
)

/**
 * Data transfer object for medlemsforespørsel
 * Brukes av klubbadmin for å se og behandle medlemsforespørsler
 */
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

