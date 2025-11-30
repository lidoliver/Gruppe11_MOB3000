package my.application.data.remote.dto

import com.squareup.moshi.Json

/**
 * Data transfer object for vennedata fra backend
 * Inneholder informasjon om vennskap, inkludert online-status og spillstatistikk
 */
data class FriendDto(
    @Json(name = "id")
    val id: Int,

    @Json(name = "user_email")
    val userEmail: String,

    @Json(name = "friend_email")
    val friendEmail: String,

    @Json(name = "status")
    val status: String,

    @Json(name = "friend_username")
    val friendUsername: String,

    @Json(name = "friend_first_name")
    val friendFirstName: String? = null,

    @Json(name = "friend_last_name")
    val friendLastName: String? = null,

    @Json(name = "games_played")
    val gamesPlayed: Int = 0,

    @Json(name = "last_played")
    val lastPlayed: String? = null,

    @Json(name = "unread_messages")
    val unreadMessages: Int = 0,

    @Json(name = "online_status")
    val onlineStatus: String? = "offline",

    @Json(name = "created_at")
    val createdAt: String? = null
)

/**
 * Data transfer object for venneforespørsel fra backend
 * Brukes for å vise ventende venneforespørsler
 */
data class FriendRequestDto(
    @Json(name = "id")
    val id: Int,

    @Json(name = "user_email")
    val userEmail: String,

    @Json(name = "friend_username")
    val friendUsername: String,

    @Json(name = "status")
    val status: String,

    @Json(name = "created_at")
    val createdAt: String
)

/**
 * Request body for sending av venneforespørsel
 */
data class SendFriendRequestDto(
    @Json(name = "friendEmail")
    val friendEmail: String
)

/**
 * Response for venneforespørsel-operasjoner
 */
data class FriendRequestResponseDto(
    @Json(name = "message")
    val message: String
)

