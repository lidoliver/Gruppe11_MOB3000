package my.application.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Detailed game history for a player on a specific course
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
 * Individual game details
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
 * Score for a specific hole in a game
 */
@JsonClass(generateAdapter = true)
data class GameHoleScore(
    @Json(name = "holeId") val holeId: Int,
    @Json(name = "score") val score: Int
)

/**
 * Aggregate statistics for a specific hole across multiple games
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

