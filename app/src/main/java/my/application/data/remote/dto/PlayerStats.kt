package my.application.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerStats(
    @Json(name = "email") val email: String,
    @Json(name = "totalGames") val totalGames: Int = 0,
    @Json(name = "averageScore") val averageScore: Double? = null,
    @Json(name = "bestScore") val bestScore: Int? = null,
    @Json(name = "worstScore") val worstScore: Int? = null,
    @Json(name = "lastPlayedAt") val lastPlayedAt: String? = null,
    @Json(name = "totalRoundsByCourse") val totalRoundsByCourse: Map<String, Int> = emptyMap()
)

