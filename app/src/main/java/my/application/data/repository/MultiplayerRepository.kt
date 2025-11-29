package my.application.data.repository
import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import my.application.data.auth.AuthManager
import my.application.data.auth.FirebaseAuthManager
import my.application.data.model.MultiplayerSession
import my.application.data.model.Player
import java.util.UUID
import kotlin.toString
class MultiplayerRepository(private val context: Context) {
    private val database: DatabaseReference = FirebaseDatabase.getInstance(
        "https://mob3000-81751-default-rtdb.europe-west1.firebasedatabase.app"
    ).reference
    private val authManager = AuthManager.getInstance(context)
    private val firebaseAuthManager = FirebaseAuthManager.getInstance()
    companion object {
        private const val TAG = "MultiplayerRepository"
        private const val SESSIONS_PATH = "game_sessions"
        @Volatile
        private var instance: MultiplayerRepository? = null
        fun getInstance(context: Context): MultiplayerRepository {
            return instance ?: synchronized(this) {
                instance ?: MultiplayerRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
        // Helper function to encode email for Firebase key
        // Firebase keys cannot contain: . $ # [ ] /
        fun encodeEmail(email: String): String {
            return email.replace(".", ",")
                .replace("@", "_at_")
        }
        // Helper function to decode Firebase key back to email
        fun decodeEmail(key: String): String {
            return key.replace(",", ".")
                .replace("_at_", "@")
        }
    }
    suspend fun createSession(
        courseId: String,
        courseName: String,
        totalHoles: Int
    ): Result<String> {
        return try {
            Log.d(TAG, "createSession called - courseId: $courseId, courseName: $courseName, totalHoles: $totalHoles")

            val userEmail = authManager.getUserEmail()
            Log.d(TAG, "User email retrieved: $userEmail")

            if (userEmail == null) {
                Log.e(TAG, "User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }

            // Ensure signed in to Firebase Auth for database access
            Log.d(TAG, "Ensuring Firebase Auth sign-in...")
            firebaseAuthManager.ensureFirebaseSignedIn(userEmail).onFailure { error ->
                Log.e(TAG, "Failed to sign in to Firebase: ${error.message}")
                return Result.failure(Exception("Failed to authenticate with Firebase: ${error.message}"))
            }
            Log.d(TAG, "Firebase Auth confirmed")

            val sessionId = UUID.randomUUID().toString()
            val encodedEmail = encodeEmail(userEmail)
            Log.d(TAG, "Session ID generated: $sessionId, Encoded email: $encodedEmail")

            // Match the backend structure exactly
            val sessionData = mapOf(
                "sessionId" to sessionId,
                "baneId" to courseId,
                "baneName" to courseName,
                "hostEmail" to userEmail,
                "status" to "waiting",
                "currentHole" to 1,
                "createdAt" to System.currentTimeMillis(),
                "players" to mapOf(
                    encodedEmail to mapOf(
                        "email" to userEmail,
                        "isHost" to true,
                        "joinedAt" to System.currentTimeMillis(),
                        "ready" to false,
                        "scores" to emptyMap<String, Any>()
                    )
                )
            )

            Log.d(TAG, "Session data prepared, writing to Firebase...")
            try {
                database.child("games")
                    .child(sessionId)
                    .setValue(sessionData)
                    .await()
                Log.d(TAG, "Session created successfully: $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Firebase write failed", e)
                // Sjekk om det er en tilgangsfeil
                if (e.message?.contains("Permission denied", ignoreCase = true) == true) {
                    throw Exception("Firebase permission denied. Please update Realtime Database rules in Firebase Console.")
                }
                throw e
            }

            Result.success(sessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating session: ${e.message}", e)
            Result.failure(e)
        }
    }
    suspend fun joinSession(sessionId: String): Result<Unit> {
        return try {
            val userEmail = authManager.getUserEmail() ?: return Result.failure(
                Exception("User not authenticated")
            )

            val encodedEmail = encodeEmail(userEmail)
            val sessionRef = database.child("games").child(sessionId)
            val snapshot = sessionRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("Session not found"))
            }

            val session = snapshot.getValue(MultiplayerSession::class.java)
                ?: return Result.failure(Exception("Invalid session data"))

            if (session.status != "waiting") {
                return Result.failure(Exception("Session already started"))
            }

            if (session.players.containsKey(encodedEmail)) {
                return Result.success(Unit)
            }

            val newPlayer = Player(
                email = userEmail,
                isHost = false,
                joinedAt = System.currentTimeMillis(),
                ready = false,
                scores = emptyMap()
            )

            sessionRef.child("players").child(encodedEmail).setValue(newPlayer).await()

            Log.d(TAG, "Joined session: $sessionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error joining session", e)
            Result.failure(e)
        }
    }
    suspend fun setPlayerReady(sessionId: String, isReady: Boolean): Result<Unit> {
        return try {
            val userEmail = authManager.getUserEmail() ?: return Result.failure(
                Exception("User not authenticated")
            )

            val encodedEmail = encodeEmail(userEmail)

            database.child("games")
                .child(sessionId)
                .child("players")
                .child(encodedEmail)
                .child("ready")
                .setValue(isReady)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting ready status", e)
            Result.failure(e)
        }
    }
    suspend fun startSession(sessionId: String): Result<Unit> {
        return try {
            val userEmail = authManager.getUserEmail() ?: return Result.failure(
                Exception("User not authenticated")
            )
            val sessionRef = database.child("games").child(sessionId)
            val snapshot = sessionRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("Session not found"))
            }
            val session = snapshot.getValue(MultiplayerSession::class.java)
                ?: return Result.failure(Exception("Invalid session data"))
            if (session.hostEmail != userEmail) {
                return Result.failure(Exception("Only host can start the game"))
            }
            if (!session.players.values.all { it.ready }) {
                return Result.failure(Exception("Not all players are ready"))
            }
            sessionRef.child("status").setValue("active").await()
            Log.d(TAG, "Session started: ")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting session", e)
            Result.failure(e)
        }
    }
    suspend fun updatePlayerScore(
        sessionId: String,
        holeNumber: Int,
        score: Int
    ): Result<Unit> {
        return try {
            val userEmail = authManager.getUserEmail() ?: return Result.failure(
                Exception("User not authenticated")
            )

            val encodedEmail = encodeEmail(userEmail)
            val scoreData = mapOf(
                "score" to score,
                "timestamp" to System.currentTimeMillis()
            )

            database.child("games")
                .child(sessionId)
                .child("players")
                .child(encodedEmail)
                .child("scores")
                .child(holeNumber.toString())
                .setValue(scoreData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating score", e)
            Result.failure(e)
        }
    }
    suspend fun updateCurrentHole(sessionId: String, holeNumber: Int): Result<Unit> {
        return try {
            val userEmail = authManager.getUserEmail() ?: return Result.failure(
                Exception("User not authenticated")
            )

            val sessionRef = database.child("games").child(sessionId)
            val snapshot = sessionRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("Session not found"))
            }

            val session = snapshot.getValue(MultiplayerSession::class.java)
                ?: return Result.failure(Exception("Invalid session data"))

            if (session.hostEmail != userEmail) {
                return Result.failure(Exception("Only host can change holes"))
            }

            sessionRef.child("currentHole").setValue(holeNumber).await()

            Log.d(TAG, "Current hole updated: $holeNumber")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating current hole", e)
            Result.failure(e)
        }
    }
    suspend fun finishSession(sessionId: String): Result<Unit> {
        return try {
            val sessionRef = database.child("games").child(sessionId)
            sessionRef.child("status").setValue("finished").await()

            Log.d(TAG, "Session finished: $sessionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error finishing session", e)
            Result.failure(e)
        }
    }
    suspend fun leaveSession(sessionId: String): Result<Unit> {
        return try {
            val userEmail = authManager.getUserEmail() ?: return Result.failure(
                Exception("User not authenticated")
            )

            val encodedEmail = encodeEmail(userEmail)
            val sessionRef = database.child("games").child(sessionId)
            val snapshot = sessionRef.get().await()

            if (!snapshot.exists()) {
                return Result.success(Unit)
            }

            val session = snapshot.getValue(MultiplayerSession::class.java)
                ?: return Result.failure(Exception("Invalid session data"))

            val playersRef = sessionRef.child("players")
            playersRef.child(encodedEmail).removeValue().await()

            // Sjekk om dette var siste spilleren
            val updatedSnapshot = playersRef.get().await()
            if (!updatedSnapshot.hasChildren()) {
                sessionRef.removeValue().await()
            } else if (session.hostEmail == userEmail) {
                // Tildel ny vert - dekod nøkkelen for å få e-posten
                val newHostKey = updatedSnapshot.children.firstOrNull()?.key
                if (newHostKey != null) {
                    val newHostEmail = decodeEmail(newHostKey)
                    sessionRef.child("hostEmail").setValue(newHostEmail).await()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving session", e)
            Result.failure(e)
        }
    }
    fun observeSession(sessionId: String): Flow<MultiplayerSession?> = callbackFlow {
        val sessionRef = database.child("games").child(sessionId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val session = snapshot.getValue(MultiplayerSession::class.java)
                trySend(session)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Session observation cancelled", error.toException())
                close(error.toException())
            }
        }

        sessionRef.addValueEventListener(listener)

        awaitClose {
            sessionRef.removeEventListener(listener)
        }
    }

    suspend fun getActiveSessions(courseId: String): Result<List<MultiplayerSession>> {
        return try {
            val snapshot = database.child("games")
                .orderByChild("baneId")
                .equalTo(courseId)
                .get()
                .await()

            val sessions = mutableListOf<MultiplayerSession>()
            snapshot.children.forEach { child ->
                child.getValue(MultiplayerSession::class.java)?.let { session ->
                    if (session.status == "waiting" || session.status == "active") {
                        sessions.add(session)
                    }
                }
            }

            Result.success(sessions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active sessions", e)
            Result.failure(e)
        }
    }
}
