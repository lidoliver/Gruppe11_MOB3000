package my.application.ui.screen.myprofile

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.application.data.auth.AuthManager
import my.application.data.remote.dto.ScorecardDto
import my.application.data.remote.dto.UserDto
import my.application.data.repository.AuthException
import my.application.data.repository.UserRepository

class MyProfileViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val userRepository: UserRepository = UserRepository.getInstance()
    private val authManager: AuthManager = AuthManager.getInstance(application)

    var uiState by mutableStateOf(MyProfileUiState())
        private set

    fun loadUserProfile(email: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, authError = false)

            val userResult = userRepository.getUser(email)
            val scorecardsResult = userRepository.getScorecards(email)

            uiState = when {
                userResult.isSuccess -> {
                    val user = userResult.getOrNull()
                    val scorecards = scorecardsResult.getOrNull() ?: emptyList()
                    uiState.copy(
                        user = user,
                        scorecards = scorecards,
                        isLoading = false,
                        error = null,
                        authError = false
                    )
                }
                userResult.exceptionOrNull() is AuthException -> {
                    // Authentication failed - clear auth data
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

            // First check if username is available
            val availabilityResult = userRepository.checkUsernameAvailability(newUsername)

            if (availabilityResult.isFailure || availabilityResult.getOrNull() == false) {
                uiState = uiState.copy(isLoading = false)
                onError("Username is already taken")
                return@launch
            }

            // Update the user with new username
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
    val isLoading: Boolean = false,
    val error: String? = null,
    val authError: Boolean = false
)
