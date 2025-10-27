package my.application.data.network.dto

import com.squareup.moshi.Json

data class PendingJoinRequestDto(
    @Json(name = "id") val id: Int,
    @Json(name = "user_email") val userEmail: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "message") val message: String?,
    @Json(name = "requested_at") val requestedAt: String
)
