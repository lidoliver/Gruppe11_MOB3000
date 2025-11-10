package my.application.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO for backend track/course data
 */
@JsonClass(generateAdapter = true)
data class TrackDto(
    @Json(name = "bane_id")
    val baneId: Int,

    @Json(name = "trackName")
    val trackName: String? = null,

    @Json(name = "navn")
    val navn: String? = null,

    @Json(name = "beskrivelse")
    val beskrivelse: String? = null,

    @Json(name = "description")
    val description: String? = null,

    @Json(name = "total_distance")
    val totalDistance: Int? = null,

    @Json(name = "totalDistance")
    val totalDistanceAlt: Int? = null,

    @Json(name = "total_par")
    val totalPar: Int? = null,

    @Json(name = "totalPar")
    val totalParAlt: Int? = null,

    @Json(name = "antall_hull")
    val antallHull: Int? = null,

    @Json(name = "holeCount")
    val holeCount: Int? = null,

    @Json(name = "klubb_id_klubb")
    val klubbIdKlubb: Int? = null,

    @Json(name = "rating")
    val rating: Int? = null,

    @Json(name = "bane_bilde")
    val baneBilde: String? = null,

    @Json(name = "opprettet_dato")
    val opprettetDato: String? = null,

    @Json(name = "created")
    val created: String? = null,

    @Json(name = "oppdatert_dato")
    val oppdatertDato: String? = null,

    @Json(name = "updated")
    val updated: String? = null
) {
    // Helper properties to get the correct value regardless of which field is populated
    val name: String
        get() = trackName ?: navn ?: "Unknown Course"

    val descriptionText: String
        get() = description ?: beskrivelse ?: ""

    val holes: Int
        get() = holeCount ?: antallHull ?: 18

    val par: Int
        get() = totalPar ?: totalParAlt ?: 0

    val distance: Int
        get() = totalDistance ?: totalDistanceAlt ?: 0
}

