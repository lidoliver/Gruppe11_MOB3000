package my.application.ui.screen.login

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.application.data.auth.AuthManager
import my.application.data.repository.UserRepository

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authManager = AuthManager.getInstance(application)
    private val userRepository = UserRepository.getInstance()

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(error = "Email and password are required")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = userRepository.login(email, password)

            uiState = when {
                result.isSuccess -> {
                    val response = result.getOrNull()!!
                    // Save token and email
                    authManager.saveAuthToken(response.token, response.email)
                    LoginUiState(
                        isLoading = false,
                        isSuccess = true,
                        userEmail = response.email,
                        error = null
                    )
                }
                else -> {
                    LoginUiState(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Login failed"
                    )
                }
            }
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(error = "Email and password are required")
            return
        }

        if (password != confirmPassword) {
            uiState = uiState.copy(error = "Passwords do not match")
            return
        }

        if (password.length < 6) {
            uiState = uiState.copy(error = "Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = userRepository.register(email, password)

            uiState = when {
                result.isSuccess -> {
                    // After registration, automatically log in
                    login(email, password)
                    uiState
                }
                else -> {
                    LoginUiState(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Registration failed"
                    )
                }
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    fun resetState() {
        uiState = LoginUiState()
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val userEmail: String? = null,
    val error: String? = null
)

