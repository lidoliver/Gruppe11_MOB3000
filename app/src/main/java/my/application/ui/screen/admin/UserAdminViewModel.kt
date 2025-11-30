package my.application.ui.screen.admin

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.application.data.remote.dto.UserDto
import my.application.data.repository.AdminRepository
import my.application.data.repository.AuthException

class UserAdminViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val adminRepository: AdminRepository = AdminRepository.getInstance(application)

    var uiState by mutableStateOf(UserAdminUiState())
        private set

    init {
        loadAllUsers()
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = adminRepository.getAllUsers()

            uiState = when {
                result.isSuccess -> {
                    uiState.copy(
                        users = result.getOrNull() ?: emptyList(),
                        filteredUsers = result.getOrNull() ?: emptyList(),
                        isLoading = false,
                        error = null
                    )
                }
                result.exceptionOrNull() is AuthException -> {
                    uiState.copy(
                        isLoading = false,
                        error = "Unauthorized access",
                        authError = true
                    )
                }
                else -> {
                    uiState.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load users"
                    )
                }
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            uiState = uiState.copy(
                filteredUsers = uiState.users,
                searchQuery = ""
            )
            return
        }

        uiState = uiState.copy(
            filteredUsers = uiState.users.filter { user ->
                user.email.contains(query, ignoreCase = true) ||
                        user.username?.contains(query, ignoreCase = true) == true ||
                        user.firstName?.contains(query, ignoreCase = true) == true ||
                        user.lastName?.contains(query, ignoreCase = true) == true
            },
            searchQuery = query
        )
    }

    fun banUser(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = adminRepository.banUser(email)

            when {
                result.isSuccess -> {
                    // Reload users after successful ban
                    loadAllUsers()
                    onSuccess()
                }
                else -> {
                    uiState = uiState.copy(isLoading = false)
                    val errorMessage = result.exceptionOrNull()?.message ?: "Failed to ban user"
                    onError(errorMessage)
                }
            }
        }
    }

    fun unbanUser(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = adminRepository.unbanUser(email)

            when {
                result.isSuccess -> {
                    // Reload users after successful unban
                    loadAllUsers()
                    onSuccess()
                }
                else -> {
                    uiState = uiState.copy(isLoading = false)
                    val errorMessage = result.exceptionOrNull()?.message ?: "Failed to unban user"
                    onError(errorMessage)
                }
            }
        }
    }

    fun deleteUser(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = adminRepository.deleteUser(email)

            when {
                result.isSuccess -> {
                    // Reload users after successful deletion
                    loadAllUsers()
                    onSuccess()
                }
                else -> {
                    uiState = uiState.copy(isLoading = false)
                    val errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete user"
                    onError(errorMessage)
                }
            }
        }
    }
}

/**
 * UI-tilstand for brukeradministrasjons-skjermen
 * @param users Liste over alle brukere
 * @param filteredUsers Filtrert liste over brukere basert på søkeord
 * @param searchQuery Gjeldende søkeord
 * @param isLoading Indikerer om data lastes
 * @param error Feilmelding hvis noe gikk galt
 * @param authError Indikerer om feilen er relatert til autentisering
 */
data class UserAdminUiState(
    val users: List<UserDto> = emptyList(),
    val filteredUsers: List<UserDto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val authError: Boolean = false
)

