package my.application.data.remote.dto

import com.squareup.moshi.Json

/**
 * Data transfer object for friend data from backend
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

    @Json(name = "created_at")
    val createdAt: String? = null
)

/**
 * Data transfer object for friend request data from backend
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
 * Request body for sending a friend request
 */
data class SendFriendRequestDto(
    @Json(name = "friendEmail")
    val friendEmail: String
)

/**
 * Response for friend request operations
 */
data class FriendRequestResponseDto(
    @Json(name = "message")
    val message: String
)

