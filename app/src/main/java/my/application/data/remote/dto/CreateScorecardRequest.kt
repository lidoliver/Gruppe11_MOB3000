package my.application.data.remote.dto

import com.squareup.moshi.Json

data class CreateScorecardRequest(
    @Json(name = "bane_id") val baneId: String,
    @Json(name = "spiller_navn") val spillerNavn: String,
    @Json(name = "hullPoeng") val hullPoeng: List<HoleScore>
)

data class HoleScore(
    @Json(name = "hull_id") val hullId: String,
    @Json(name = "poeng") val poeng: Int
)

