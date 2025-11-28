package my.application.ui.screen.club

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.application.data.auth.AuthManager
import my.application.data.remote.dto.ClubJoinRequestDto
import my.application.data.repository.ClubRepository
import my.application.data.repository.UserRepository

class ClubAdminPanelViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val clubRepository: ClubRepository = ClubRepository.getInstance()
    private val userRepository: UserRepository = UserRepository.getInstance()
    private val authManager: AuthManager = AuthManager.getInstance(application)

    var uiState by mutableStateOf(ClubAdminPanelUiState())
        private set

    init {
        loadUserClubAndRequests()
    }

    private fun loadUserClubAndRequests() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            // Get current user's email
            val userEmail = authManager.getUserEmail()
            if (userEmail == null) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
                return@launch
            }

            // Get user details to find their club ID
            val userResult = userRepository.getUser(userEmail)

            userResult.fold(
                onSuccess = { user ->
                    val clubId = user.clubId
                    if (clubId == null || clubId == 0) {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = "Du er ikke tilknyttet noen klubb"
                        )
                        return@fold
                    }

                    uiState = uiState.copy(clubId = clubId)
                    loadJoinRequests()
                },
                onFailure = { error ->
                    Log.e("ClubAdminPanelViewModel", "Failed to load user", error)
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "Kunne ikke hente brukerinformasjon"
                    )
                }
            )
        }
    }

    fun loadJoinRequests() {
        val clubId = uiState.clubId
        if (clubId == null) {
            Log.e("ClubAdminPanelViewModel", "No club ID available")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = clubRepository.getClubJoinRequests(clubId)

            uiState = when {
                result.isSuccess -> {
                    uiState.copy(
                        requests = result.getOrNull() ?: emptyList(),
                        isLoading = false,
                        error = null
                    )
                }
                else -> {
                    uiState.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load requests"
                    )
                }
            }
        }
    }

    fun approveRequest(requestId: Int) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = clubRepository.approveJoinRequest(requestId)

            uiState = when {
                result.isSuccess -> {
                    Log.d("ClubAdminPanelViewModel", "Request approved successfully")
                    // Remove the approved request from the list
                    uiState.copy(
                        requests = uiState.requests.filter { it.id != requestId },
                        isLoading = false,
                        successMessage = "Søknad godkjent!"
                    )
                }
                else -> {
                    Log.e("ClubAdminPanelViewModel", "Failed to approve request", result.exceptionOrNull())
                    uiState.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Kunne ikke godkjenne søknad"
                    )
                }
            }
        }
    }

    fun rejectRequest(requestId: Int) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = clubRepository.rejectJoinRequest(requestId)

            uiState = when {
                result.isSuccess -> {
                    Log.d("ClubAdminPanelViewModel", "Request rejected successfully")
                    // Remove the rejected request from the list
                    uiState.copy(
                        requests = uiState.requests.filter { it.id != requestId },
                        isLoading = false,
                        successMessage = "Søknad avvist"
                    )
                }
                else -> {
                    Log.e("ClubAdminPanelViewModel", "Failed to reject request", result.exceptionOrNull())
                    uiState.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Kunne ikke avvise søknad"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(successMessage = null, error = null)
    }
}

data class ClubAdminPanelUiState(
    val clubId: Int? = null,
    val requests: List<ClubJoinRequestDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

