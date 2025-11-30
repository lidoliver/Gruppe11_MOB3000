package my.application.data.remote.dto

import com.squareup.moshi.Json

/**
 * Request body for opprettelse av spillkort
 * Sendes til backend etter fullført runde for å lagre resultater
 */
data class CreateScorecardRequest(
    @Json(name = "bane_id") val baneId: String,
    @Json(name = "spiller_navn") val spillerNavn: String,
    @Json(name = "hullPoeng") val hullPoeng: List<HoleScore>
)

/**
 * Poengsum for et enkelt hull i spillkortet
 */
data class HoleScore(
    @Json(name = "hull_id") val hullId: String,
    @Json(name = "poeng") val poeng: Int
)

