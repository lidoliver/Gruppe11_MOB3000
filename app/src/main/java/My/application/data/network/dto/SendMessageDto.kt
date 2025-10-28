package my.application.data.network.dto

import com.squareup.moshi.Json

data class SendMessageDto(
    @Json(name = "receiverEmail") val receiverEmail: String,
    @Json(name = "content") val content: String
)
