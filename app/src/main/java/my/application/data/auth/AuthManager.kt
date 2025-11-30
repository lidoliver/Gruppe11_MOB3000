package my.application.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import my.application.data.repository.UserRepository

// DataStore for persistent lagring av autentiseringsdata
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Håndterer autentisering og lagring av brukerdata
 * Bruker DataStore for sikker og persistent lagring av JWT-token og e-post
 */
class AuthManager(private val context: Context) {

    private val userRepository = UserRepository.getInstance()

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val EMAIL_KEY = stringPreferencesKey("user_email")

        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Lagrer autentiseringstoken og brukerens e-post
     * Oppdaterer også UserRepository med samme informasjon
     * @param token JWT-token fra backend
     * @param email Brukerens e-postadresse
     */
    suspend fun saveAuthToken(token: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[EMAIL_KEY] = email
        }
        userRepository.setAuthToken(token)
        userRepository.setUserEmail(email)
    }

    /**
     * Henter lagret autentiseringstoken
     * @return JWT-token eller null hvis ikke innlogget
     */
    suspend fun getAuthToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }

    /**
     * Henter lagret bruker e-post
     * @return E-postadresse eller null hvis ikke innlogget
     */
    suspend fun getUserEmail(): String? {
        return context.dataStore.data.first()[EMAIL_KEY]
    }

    /**
     * Henter autentiseringstoken som Flow for reaktiv oppdatering
     * @return Flow med JWT-token som oppdateres automatisk
     */
    fun getAuthTokenFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }

    /**
     * Henter bruker e-post som Flow for reaktiv oppdatering
     * @return Flow med e-postadresse som oppdateres automatisk
     */
    fun getUserEmailFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[EMAIL_KEY]
        }
    }

    /**
     * Sjekker om bruker er innlogget
     * @return true hvis både token og e-post er lagret
     */
    suspend fun isLoggedIn(): Boolean {
        val token = getAuthToken()
        val email = getUserEmail()
        return !token.isNullOrEmpty() && !email.isNullOrEmpty()
    }

    /**
     * Fjerner all autentiseringsdata (logger ut bruker)
     * Sletter token og e-post fra både DataStore og UserRepository
     */
    suspend fun clearAuth() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(EMAIL_KEY)
        }
        userRepository.setAuthToken("")
    }

    /**
     * Initialiserer autentisering ved app-oppstart
     * Laster inn lagret token og e-post og setter dem i UserRepository
     */
    suspend fun initializeAuth() {
        val token = getAuthToken()
        val email = getUserEmail()
        if (!token.isNullOrEmpty()) {
            userRepository.setAuthToken(token)
        }
        if (!email.isNullOrEmpty()) {
            userRepository.setUserEmail(email)
        }
    }
}

