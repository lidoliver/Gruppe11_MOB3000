package my.application.data.local

import kotlin.random.Random

data class LocalMultiplayerSession(
    val sessionId: String,
    val courseId: String,
    val courseName: String,
    val totalHoles: Int,
    val isHost: Boolean
)

/**
 * Very simple in-memory session holder. This lets us get multiplayer
 * creation/join flows working without depending on Firebase client SDK.
 */
object MultiplayerSessionManager {

    @Volatile
    private var currentSession: LocalMultiplayerSession? = null

    fun createSession(courseId: String, courseName: String, totalHoles: Int): LocalMultiplayerSession {
        val sessionId = generateSessionCode()
        val session = LocalMultiplayerSession(
            sessionId = sessionId,
            courseId = courseId,
            courseName = courseName,
            totalHoles = totalHoles,
            isHost = true
        )
        currentSession = session
        return session
    }

    fun joinSession(sessionId: String, courseId: String, courseName: String, totalHoles: Int): LocalMultiplayerSession {
        val session = LocalMultiplayerSession(
            sessionId = sessionId,
            courseId = courseId,
            courseName = courseName,
            totalHoles = totalHoles,
            isHost = false
        )
        currentSession = session
        return session
    }

    fun getCurrentSession(): LocalMultiplayerSession? = currentSession

    fun clear() {
        currentSession = null
    }

    private fun generateSessionCode(length: Int = 6): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..length)
            .map { chars.random(Random) }
            .joinToString("")
    }
}

