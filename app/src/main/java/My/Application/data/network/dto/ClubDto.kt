package my.application.data.network.dto

import com.squareup.moshi.Json

data class ClubDto(
    @Json(name = "id") val id: Int,
    @Json(name = "navn") val name: String,
    @Json(name = "beskrivelse") val description: String?,
    @Json(name = "sted") val location: String?,
    @Json(name = "admin_email") val adminEmail: String
)
