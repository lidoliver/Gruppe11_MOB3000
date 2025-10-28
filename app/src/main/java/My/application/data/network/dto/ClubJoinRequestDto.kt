package my.application.data.network.dto

import com.squareup.moshi.Json

data class ClubJoinRequestDto(
    @Json(name = "klubId") val clubId: Int,
    @Json(name = "message") val message: String?
)
