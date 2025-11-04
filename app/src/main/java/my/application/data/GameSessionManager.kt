package my.application.data

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class GameSession(
    val courseId: String,
    val sessionId: String?,
    val courseName: String,
    val currentHole: Int,
    val totalHoles: Int,
    val holeScores: List<Int>,
    val timestamp: Long = System.currentTimeMillis()
)

class GameSessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("game_session_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val jsonAdapter = moshi.adapter(GameSession::class.java)

    companion object {
        private const val KEY_ACTIVE_SESSION = "active_game_session"
        private const val KEY_HAS_ACTIVE_SESSION = "has_active_session"
    }

    fun saveGameSession(session: GameSession) {
        val json = jsonAdapter.toJson(session)
        prefs.edit()
            .putString(KEY_ACTIVE_SESSION, json)
            .putBoolean(KEY_HAS_ACTIVE_SESSION, true)
            .apply()
    }

    fun getGameSession(): GameSession? {
        val json = prefs.getString(KEY_ACTIVE_SESSION, null) ?: return null
        return try {
            jsonAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun hasActiveSession(): Boolean {
        return prefs.getBoolean(KEY_HAS_ACTIVE_SESSION, false)
    }

    fun clearGameSession() {
        prefs.edit()
            .remove(KEY_ACTIVE_SESSION)
            .putBoolean(KEY_HAS_ACTIVE_SESSION, false)
            .apply()
    }

    fun updateHoleScore(holeIndex: Int, score: Int) {
        val session = getGameSession() ?: return
        val updatedScores = session.holeScores.toMutableList().apply {
            this[holeIndex] = score
        }
        saveGameSession(session.copy(holeScores = updatedScores))
    }

    fun updateCurrentHole(hole: Int) {
        val session = getGameSession() ?: return
        saveGameSession(session.copy(currentHole = hole))
    }
}

