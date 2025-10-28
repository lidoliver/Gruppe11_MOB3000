package my.application.data.network.dto

import com.squareup.moshi.Json

data class MessageResponseDto(
    @Json(name = "message") val message: String
)
