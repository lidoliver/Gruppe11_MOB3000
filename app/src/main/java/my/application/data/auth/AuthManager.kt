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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

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

    suspend fun saveAuthToken(token: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[EMAIL_KEY] = email
        }
        userRepository.setAuthToken(token)
    }

    suspend fun getAuthToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }

    suspend fun getUserEmail(): String? {
        return context.dataStore.data.first()[EMAIL_KEY]
    }

    fun getAuthTokenFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }

    fun getUserEmailFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[EMAIL_KEY]
        }
    }

    suspend fun isLoggedIn(): Boolean {
        val token = getAuthToken()
        val email = getUserEmail()
        return !token.isNullOrEmpty() && !email.isNullOrEmpty()
    }

    suspend fun clearAuth() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(EMAIL_KEY)
        }
        userRepository.setAuthToken("")
    }

    suspend fun initializeAuth() {
        val token = getAuthToken()
        if (!token.isNullOrEmpty()) {
            userRepository.setAuthToken(token)
        }
    }
}

