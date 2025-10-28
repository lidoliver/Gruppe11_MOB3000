package my.application.data.network.dto

import com.squareup.moshi.Json

data class ClubRequestResponseDto(
    @Json(name = "message") val message: String,
    @Json(name = "requestId") val requestId: Int
)
