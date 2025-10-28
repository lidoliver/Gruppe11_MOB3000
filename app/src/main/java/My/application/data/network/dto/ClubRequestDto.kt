package my.application.data.network.dto

import com.squareup.moshi.Json

data class ClubRequestDto(
    @Json(name = "id") val id: Int,
    @Json(name = "user_email") val userEmail: String,
    @Json(name = "klubbnavn") val clubName: String,
    @Json(name = "sted") val location: String?,
    @Json(name = "beskrivelse") val description: String?,
    @Json(name = "status") val status: String,
    @Json(name = "created_at") val createdAt: String
)
