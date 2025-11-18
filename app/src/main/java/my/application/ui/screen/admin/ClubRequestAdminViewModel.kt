package my.application.ui.screen.admin

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.application.data.remote.dto.ClubRequestDto
import my.application.data.repository.AdminRepository
import my.application.data.repository.AuthException

class ClubRequestAdminViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val adminRepository: AdminRepository = AdminRepository.getInstance(application)

    var uiState by mutableStateOf(ClubRequestAdminUiState())
        private set

    init {
        loadClubRequests()
    }

    fun loadClubRequests() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = adminRepository.getClubRequests()

            uiState = when {
                result.isSuccess -> {
                    uiState.copy(
                        clubRequests = result.getOrNull() ?: emptyList(),
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
                        error = result.exceptionOrNull()?.message ?: "Failed to load club requests"
                    )
                }
            }
        }
    }

    fun approveClubRequest(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = adminRepository.approveClubRequest(id)

            when {
                result.isSuccess -> {
                    // Reload club requests after successful approval
                    loadClubRequests()
                    onSuccess()
                }
                else -> {
                    uiState = uiState.copy(isLoading = false)
                    val errorMessage = result.exceptionOrNull()?.message ?: "Failed to approve club request"
                    onError(errorMessage)
                }
            }
        }
    }

    fun rejectClubRequest(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = adminRepository.rejectClubRequest(id)

            when {
                result.isSuccess -> {
                    // Reload club requests after successful rejection
                    loadClubRequests()
                    onSuccess()
                }
                else -> {
                    uiState = uiState.copy(isLoading = false)
                    val errorMessage = result.exceptionOrNull()?.message ?: "Failed to reject club request"
                    onError(errorMessage)
                }
            }
        }
    }
}

data class ClubRequestAdminUiState(
    val clubRequests: List<ClubRequestDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val authError: Boolean = false
)

