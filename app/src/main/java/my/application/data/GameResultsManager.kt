package my.application.data

import android.content.Context

/**
 * Manager for storing and retrieving game results temporarily
 * Used to pass score data to post-game screen
 */
object GameResultsManager {
    private var currentGameResults: GameResults? = null

    fun setGameResults(results: GameResults) {
        currentGameResults = results
    }

    fun getGameResults(): GameResults? {
        return currentGameResults
    }

    fun clearGameResults() {
        currentGameResults = null
    }
}

data class GameResults(
    val courseName: String,
    val totalHoles: Int,
    val playerScores: Map<String, List<Int>>, // email to list of scores per hole
    val isSinglePlayer: Boolean
)

