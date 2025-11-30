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
import my.application.data.remote.dto.ClubDto
import my.application.data.repository.ClubRepository
import my.application.data.repository.UserRepository

class ClubListViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val clubRepository: ClubRepository = ClubRepository.getInstance()
    private val userRepository: UserRepository = UserRepository.getInstance()
    private val authManager: AuthManager = AuthManager.getInstance(application)

    var uiState by mutableStateOf(ClubListUiState())
        private set

    init {
        loadClubs()
    }

    fun loadClubs() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            // First get user's club ID if they have one
            val userEmail = authManager.getUserEmail()
            var userClubId: Int? = null

            if (userEmail != null) {
                val userResult = userRepository.getUser(userEmail)
                userResult.fold(
                    onSuccess = { user ->
                        userClubId = user.clubId
                        Log.d("ClubListViewModel", "User's club ID: $userClubId")
                    },
                    onFailure = { error ->
                        Log.e("ClubListViewModel", "Failed to get user info", error)
                    }
                )
            }

            // Then get all clubs
            val result = clubRepository.getAllClubs()

            uiState = when {
                result.isSuccess -> {
                    val allClubs = result.getOrNull() ?: emptyList()
                    // Filter out user's club if they have one
                    val filteredClubs = if (userClubId != null && userClubId!! > 0) {
                        allClubs.filter { it.id != userClubId }
                    } else {
                        allClubs
                    }

                    Log.d("ClubListViewModel", "Loaded ${allClubs.size} clubs, showing ${filteredClubs.size} (filtered out club ID: $userClubId)")

                    uiState.copy(
                        clubs = filteredClubs,
                        isLoading = false,
                        error = null
                    )
                }
                else -> {
                    uiState.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load clubs"
                    )
                }
            }
        }
    }

    fun submitJoinRequest(clubId: Int, message: String?) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = clubRepository.submitClubJoinRequest(clubId, message)

            uiState = when {
                result.isSuccess -> {
                    Log.d("ClubListViewModel", "Join request submitted successfully")
                    uiState.copy(
                        isLoading = false,
                        successMessage = "Søknad sendt!"
                    )
                }
                else -> {
                    Log.e("ClubListViewModel", "Failed to submit join request", result.exceptionOrNull())
                    uiState.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Kunne ikke sende søknad"
                    )
                }
            }
        }
    }

    /**
     * Tømmer feil- og suksessmeldinger fra UI-tilstanden
     */
    fun clearMessages() {
        uiState = uiState.copy(successMessage = null, error = null)
    }
}

/**
 * UI-tilstand for klubbliste-skjermen
 * @param clubs Liste over klubber
 * @param isLoading Indikerer om data lastes
 * @param error Feilmelding hvis noe gikk galt
 * @param successMessage Suksessmelding
 */
data class ClubListUiState(
    val clubs: List<ClubDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

