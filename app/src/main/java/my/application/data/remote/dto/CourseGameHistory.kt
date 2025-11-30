package my.application.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Detaljert spillhistorikk for en spesifikk bane for en spiller
 * Inneholder alle spill og statistikk per hull
 */
@JsonClass(generateAdapter = true)
data class CourseGameHistory(
    @Json(name = "email") val email: String,
    @Json(name = "courseName") val courseName: String,
    @Json(name = "totalGames") val totalGames: Int = 0,
    @Json(name = "games") val games: List<GameDetail> = emptyList(),
    @Json(name = "holeStatistics") val holeStatistics: List<HoleStatistic> = emptyList()
)

/**
 * Individuelle spilldetaljer
 * Representerer ett enkelt spill med alle hullresultater
 */
@JsonClass(generateAdapter = true)
data class GameDetail(
    @Json(name = "gameId") val gameId: Int,
    @Json(name = "courseId") val courseId: Int,
    @Json(name = "totalScore") val totalScore: Int,
    @Json(name = "coursePar") val coursePar: Int?,
    @Json(name = "playedAt") val playedAt: String,
    @Json(name = "holeScores") val holeScores: List<GameHoleScore> = emptyList()
)

/**
 * Poengsum for et spesifikt hull i et spill
 */
@JsonClass(generateAdapter = true)
data class GameHoleScore(
    @Json(name = "holeId") val holeId: Int,
    @Json(name = "score") val score: Int
)

/**
 * Aggregert statistikk for et spesifikt hull på tvers av flere spill
 * Viser gjennomsnitt, beste og verste score for hullet
 */
@JsonClass(generateAdapter = true)
data class HoleStatistic(
    @Json(name = "holeId") val holeId: Int,
    @Json(name = "holeNumber") val holeNumber: Int,
    @Json(name = "par") val par: Int?,
    @Json(name = "distance") val distance: Int?,
    @Json(name = "averageScore") val averageScore: String?,
    @Json(name = "bestScore") val bestScore: Int?,
    @Json(name = "worstScore") val worstScore: Int?
)

