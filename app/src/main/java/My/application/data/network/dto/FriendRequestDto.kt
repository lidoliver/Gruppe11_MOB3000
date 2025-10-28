package my.application.data.network.dto

import com.squareup.moshi.Json

data class FriendRequestDto(
    @Json(name = "id") val id: Int,
    @Json(name = "user_email") val userEmail: String,
    @Json(name = "friend_email") val friendEmail: String,
    @Json(name = "status") val status: String,
    @Json(name = "created_at") val createdAt: String
)
