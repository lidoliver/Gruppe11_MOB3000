package my.application.data.network.dto

import com.squareup.moshi.Json

data class ConversationDto(
    @Json(name = "contact_email") val contactEmail: String,
    @Json(name = "contact_name") val contactName: String,
    @Json(name = "last_message") val lastMessage: String?,
    @Json(name = "last_message_at") val lastMessageAt: String?
)
