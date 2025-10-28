package my.application.data.network.dto

import com.squareup.moshi.Json

data class ClubJoinResponseDto(
    @Json(name = "message") val message: String,
    @Json(name = "requestId") val requestId: Int
)
