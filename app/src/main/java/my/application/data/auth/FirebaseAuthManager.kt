package my.application.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Handles Firebase Authentication for Realtime Database access
 * Works alongside the existing JWT authentication system
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
     * Sign in to Firebase Auth anonymously or with custom token
     * This is needed for Realtime Database to recognize the user
     */
    suspend fun ensureFirebaseSignedIn(userEmail: String): Result<FirebaseUser> {
        return try {
            // Check if already signed in
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "Already signed in to Firebase: ${currentUser.uid}")
                return Result.success(currentUser)
            }

            // Sign in anonymously (simplest approach)
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
     * Get current Firebase user
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Check if signed in to Firebase
     */
    fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Sign out from Firebase
     */
    fun signOut() {
        firebaseAuth.signOut()
        Log.d(TAG, "Signed out from Firebase")
    }
}

