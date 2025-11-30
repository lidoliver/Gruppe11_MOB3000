package my.application.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Håndterer Firebase-autentisering for tilgang til Realtime Database
 * Fungerer sammen med det eksisterende JWT-autentiseringssystemet
 * Bruker anonym innlogging for å gi tilgang til Firebase-tjenester
 */
class FirebaseAuthManager private constructor() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "FirebaseAuthManager"

        @Volatile
        private var instance: FirebaseAuthManager? = null

        fun getInstance(): FirebaseAuthManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthManager().also { instance = it }
            }
        }
    }

    /**
     * Logger inn på Firebase Auth anonymt
     * Dette er nødvendig for at Realtime Database skal gjenkjenne brukeren
     * @param userEmail Brukerens e-postadresse (for logging)
     * @return Result med FirebaseUser ved suksess
     */
    suspend fun ensureFirebaseSignedIn(userEmail: String): Result<FirebaseUser> {
        return try {
            // Sjekk om allerede innlogget
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "Already signed in to Firebase: ${currentUser.uid}")
                return Result.success(currentUser)
            }

            // Logg inn anonymt (enkleste tilnærming)
            Log.d(TAG, "Signing in to Firebase anonymously for user: $userEmail")
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user

            if (user != null) {
                Log.d(TAG, "Successfully signed in to Firebase: ${user.uid}")
                Result.success(user)
            } else {
                Log.e(TAG, "Firebase sign-in returned null user")
                Result.failure(Exception("Firebase sign-in failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in to Firebase", e)
            Result.failure(e)
        }
    }

    /**
     * Henter nåværende Firebase-bruker
     * @return FirebaseUser eller null hvis ikke innlogget
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Sjekker om bruker er innlogget på Firebase
     * @return true hvis innlogget
     */
    fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Logger ut fra Firebase
     */
    fun signOut() {
        firebaseAuth.signOut()
        Log.d(TAG, "Signed out from Firebase")
    }
}

