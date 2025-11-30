package my.application.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the active multiplayer session to prevent multiple concurrent lobbies
 */
class ActiveSessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "active_session_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_ACTIVE_SESSION_ID = "active_session_id"
        private const val KEY_IS_HOST = "is_host"

        @Volatile
        private var instance: ActiveSessionManager? = null

        fun getInstance(context: Context): ActiveSessionManager {
            return instance ?: synchronized(this) {
                instance ?: ActiveSessionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Set the active session ID
     */
    fun setActiveSession(sessionId: String, isHost: Boolean = false) {
        prefs.edit()
            .putString(KEY_ACTIVE_SESSION_ID, sessionId)
            .putBoolean(KEY_IS_HOST, isHost)
            .apply()
    }

    /**
     * Get the active session ID if any
     */
    fun getActiveSessionId(): String? {
        return prefs.getString(KEY_ACTIVE_SESSION_ID, null)
    }

    /**
     * Check if user is host of active session
     */
    fun isHost(): Boolean {
        return prefs.getBoolean(KEY_IS_HOST, false)
    }

    /**
     * Check if there is an active session
     */
    fun hasActiveSession(): Boolean {
        return getActiveSessionId() != null
    }

    /**
     * Clear the active session
     */
    fun clearActiveSession() {
        prefs.edit()
            .remove(KEY_ACTIVE_SESSION_ID)
            .remove(KEY_IS_HOST)
            .apply()
    }
}

