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
import my.application.data.repository.UserRepository

class ClubScreenViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val userRepository: UserRepository = UserRepository.getInstance()
    private val authManager: AuthManager = AuthManager.getInstance(application)

    var uiState by mutableStateOf(ClubScreenUiState())
        private set

    init {
        loadUserClubInfo()
    }

    fun refresh() {
        loadUserClubInfo()
    }

    fun loadUserClubInfo() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val userEmail = authManager.getUserEmail()
            if (userEmail == null) {
                uiState = uiState.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    error = "Not authenticated"
                )
                return@launch
            }

            uiState = uiState.copy(isAuthenticated = true)

            val result = userRepository.getUser(userEmail)

            result.fold(
                onSuccess = { user ->
                    Log.d("ClubScreenViewModel", "User loaded: clubId=${user.clubId}, clubName=${user.clubName}")
                    uiState = uiState.copy(
                        isLoading = false,
                        clubId = user.clubId,
                        clubName = user.clubName,
                        isMember = user.clubId != null && user.clubId > 0,
                        error = null
                    )
                },
                onFailure = { error ->
                    Log.e("ClubScreenViewModel", "Failed to load user", error)
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "Kunne ikke hente brukerinformasjon"
                    )
                }
            )
        }
    }
}

/**
 * UI-tilstand for klubbskjermen
 * @param isLoading Indikerer om data lastes
 * @param isAuthenticated Indikerer om brukeren er autentisert
 * @param clubId ID-en til brukerens klubb (hvis medlem)
 * @param clubName Navnet på brukerens klubb (hvis medlem)
 * @param isMember Indikerer om brukeren er medlem av en klubb
 * @param error Feilmelding hvis noe gikk galt
 */
data class ClubScreenUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = true, // Anta autentisert som standard for bakoverkompatibilitet
    val clubId: Int? = null,
    val clubName: String? = null,
    val isMember: Boolean = false,
    val error: String? = null
)

