package my.application.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data transfer object for klubbkunngjøring
 * Brukes på klubbsiden for å vise viktige meldinger til medlemmer
 */
@JsonClass(generateAdapter = true)
data class ClubAnnouncementDto(
    @Json(name = "id") val id: Int,
    @Json(name = "klubb_id") val klubbId: Int,
    @Json(name = "content") val content: String,
    @Json(name = "author_email") val authorEmail: String?,
    @Json(name = "author_name") val authorName: String?,
    @Json(name = "created_at") val createdAt: String
)

/**
 * Data transfer object for klubbarrangement
 * Representerer et arrangement som klubben arrangerer
 */
@JsonClass(generateAdapter = true)
data class ClubEventDto(
    @Json(name = "id") val id: Int,
    @Json(name = "klubb_id") val klubbId: Int,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "event_date") val eventDate: String,
    @Json(name = "author_email") val authorEmail: String?,
    @Json(name = "author_name") val authorName: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "participant_count") val participantCount: Int,
    @Json(name = "is_participating") val isParticipating: Boolean = false
)

/**
 * Data transfer object for komplett klubbside
 * Inneholder klubbinfo, kunngjøringer og arrangementer
 */
@JsonClass(generateAdapter = true)
data class ClubPageDto(
    @Json(name = "club") val club: ClubDto,
    @Json(name = "announcements") val announcements: List<ClubAnnouncementDto>,
    @Json(name = "events") val events: List<ClubEventDto>,
    @Json(name = "isAdmin") val isAdmin: Boolean
)

/**
 * Data transfer object for arrangementsdeltaker
 * Brukes for å vise hvem som deltar i et arrangement
 */
@JsonClass(generateAdapter = true)
data class EventParticipantDto(
    @Json(name = "event_id") val eventId: Int,
    @Json(name = "user_email") val userEmail: String,
    @Json(name = "joined_at") val joinedAt: String,
    @Json(name = "brukernavn") val username: String?,
    @Json(name = "fnavn") val firstName: String?,
    @Json(name = "enavn") val lastName: String?
)

/**
 * Request body for opprettelse av kunngjøring
 */
@JsonClass(generateAdapter = true)
data class CreateAnnouncementRequest(
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class CreateEventRequest(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "eventDate") val eventDate: String
)

