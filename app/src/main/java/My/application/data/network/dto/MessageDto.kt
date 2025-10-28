package my.application.data.network.dto

import com.squareup.moshi.Json

data class MessageDto(
    @Json(name = "id") val id: Int,
    @Json(name = "sender_email") val senderEmail: String,
    @Json(name = "receiver_email") val receiverEmail: String,
    @Json(name = "content") val content: String,
    @Json(name = "created_at") val createdAt: String
)
