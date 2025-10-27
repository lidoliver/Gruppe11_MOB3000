package my.application.data.network.dto

import com.squareup.moshi.Json

data class CreateClubRequestDto(
    @Json(name = "klubbnavn") val clubName: String,
    @Json(name = "sted") val location: String?,
    @Json(name = "beskrivelse") val description: String?
)
