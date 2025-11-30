/**
 * ViewModel for min profil-skjermen
 */
package my.application.ui.screen.myprofile

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import my.application.data.auth.AuthManager
import my.application.data.remote.dto.PlayerStats
import my.application.data.remote.dto.ScorecardDto
import my.application.data.remote.dto.UserDto
import my.application.data.remote.RetrofitProvider
import my.application.data.repository.AuthException
import my.application.data.repository.UserRepository

class MyProfileViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val userRepository: UserRepository = UserRepository.getInstance()
    private val authManager: AuthManager = AuthManager.getInstance(application)
    private val playerApi = RetrofitProvider.playerApi
    private val sharedPreferences = application.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    var uiState by mutableStateOf(MyProfileUiState())
        private set

    private val _stats = MutableStateFlow<PlayerStats?>(null)
    val stats: StateFlow<PlayerStats?> = _stats.asStateFlow()

    fun loadUserProfile(email: String) {
        viewModelScope.launch {
            Log.d("MyProfileViewModel", "Loading profile for email: $email")
            uiState = uiState.copy(isLoading = true, error = null, authError = false)

            val userResult = userRepository.getUser(email)
            val scorecardsResult = userRepository.getScorecards(email)
            val friendsResult = userRepository.getFriends()

            Log.d("MyProfileViewModel", "User result: ${userResult.isSuccess}")
            Log.d("MyProfileViewModel", "Scorecards result: ${scorecardsResult.isSuccess}, count: ${scorecardsResult.getOrNull()?.size ?: 0}")
            Log.d("MyProfileViewModel", "Friends result: ${friendsResult.isSuccess}, count: ${friendsResult.getOrNull()?.size ?: 0}")

            uiState = when {
                userResult.isSuccess -> {
                    val user = userResult.getOrNull()
                    val scorecards = scorecardsResult.getOrNull() ?: emptyList()
                    val friends = friendsResult.getOrNull() ?: emptyList()

                    // Last inn lagret profilfarge fra SharedPreferences, eller generer ny
                    val savedColor = sharedPreferences.getLong("profile_color_$email", -1L)
                    val profileColor = if (savedColor != -1L) {
                        savedColor
                    } else {
                        generateColorForUser(user?.username ?: user?.email ?: "")
                    }

                    Log.d("MyProfileViewModel", "Profile loaded successfully with ${scorecards.size} scorecards and ${friends.size} friends")
                    uiState.copy(
                        user = user,
                        scorecards = scorecards,
                        friendsCount = friends.size,
                        profileColor = profileColor,
                        isLoading = false,
                        error = null,
                        authError = false
                    )
                }
                userResult.exceptionOrNull() is AuthException -> {
                    // Autentisering feilet - slett autentiseringsdata
                    authManager.clearAuth()
                    uiState.copy(
                        isLoading = false,
                        error = null,
                        authError = true
                    )
                }
                else -> {
                    uiState.copy(
                        isLoading = false,
                        error = userResult.exceptionOrNull()?.message ?: "Failed to load profile",
                        authError = false
                    )
                }
            }
        }
    }

    fun loadPlayerStats(email: String) {
        viewModelScope.launch {
            try {
                val statsData = playerApi.getPlayerStats(email)
                _stats.value = statsData
                Log.d("MyProfileViewModel", "Loaded stats: ${statsData.totalGames} games")
            } catch (e: Exception) {
                Log.e("MyProfileViewModel", "Failed to load player stats", e)
                _stats.value = null
            }
        }
    }

    fun updateProfileColor(color: Long) {
        // Oppdater UI-tilstand
        uiState = uiState.copy(profileColor = color)

        // Lagre til SharedPreferences for persistens
        uiState.user?.email?.let { email ->
            sharedPreferences.edit().putLong("profile_color_$email", color).apply()
            Log.d("MyProfileViewModel", "Profile color saved: $color for user: $email")
        }
    }

    private fun generateColorForUser(identifier: String): Long {
        val colors = listOf(
            0xFF1976D2, // Blue
            0xFF388E3C, // Green
            0xFFD32F2F, // Red
            0xFF7B1FA2, // Purple
            0xFFF57C00, // Orange
            0xFF0097A7, // Cyan
            0xFFC2185B, // Pink
            0xFF5D4037, // Brown
            0xFF455A64  // Blue Grey
        )

        val hash = identifier.hashCode()
        val index = Math.abs(hash) % colors.size
        return colors[index]
    }

    fun updateProfile(email: String, updatedUser: UserDto) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = userRepository.updateUser(email, updatedUser)

            uiState = when {
                result.isSuccess -> {
                    uiState.copy(
                        user = result.getOrNull(),
                        isLoading = false,
                        error = null
                    )
                }
                else -> {
                    uiState.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to update profile"
                    )
                }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = userRepository.changePassword(currentPassword, newPassword)

            uiState = uiState.copy(isLoading = false)

            when {
                result.isSuccess -> {
                    onSuccess()
                }
                else -> {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Failed to change password"
                    onError(errorMessage)
                }
            }
        }
    }

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return userRepository.checkUsernameAvailability(username)
    }

    fun updateUsername(email: String, newUsername: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            // Sjekk først om brukernavnet er tilgjengelig
            val availabilityResult = userRepository.checkUsernameAvailability(newUsername)

            if (availabilityResult.isFailure || availabilityResult.getOrNull() == false) {
                uiState = uiState.copy(isLoading = false)
                onError("Username is already taken")
                return@launch
            }

            // Oppdater brukeren med nytt brukernavn
            val currentUser = uiState.user ?: run {
                uiState = uiState.copy(isLoading = false)
                onError("User data not loaded")
                return@launch
            }

            val updatedUser = currentUser.copy(username = newUsername)
            val result = userRepository.updateUser(email, updatedUser)

            when {
                result.isSuccess -> {
                    uiState = uiState.copy(
                        user = result.getOrNull(),
                        isLoading = false,
                        error = null
                    )
                    onSuccess()
                }
                else -> {
                    uiState = uiState.copy(isLoading = false)
                    val errorMessage = result.exceptionOrNull()?.message ?: "Failed to update username"
                    onError(errorMessage)
                }
            }
        }
    }
}

data class MyProfileUiState(
    val user: UserDto? = null,
    val scorecards: List<ScorecardDto> = emptyList(),
    val friendsCount: Int = 0,
    val profileColor: Long = 0xFF1976D2, // Default blue color
    val isLoading: Boolean = false,
    val error: String? = null,
    val authError: Boolean = false
)
