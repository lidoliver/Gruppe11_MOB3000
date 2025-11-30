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

/**
 * ViewModel for å håndtere klubbforespørsler i admin-panelet
 * Administrerer lasting, godkjenning og avvisning av klubbforespørsler
 */
class ClubRequestAdminViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val adminRepository: AdminRepository = AdminRepository.getInstance(application)

    var uiState by mutableStateOf(ClubRequestAdminUiState())
        private set

    init {
        loadClubRequests()
    }

    /**
     * Laster inn alle ventende klubbforespørsler fra backend
     * Håndterer også 500-feil og "Failed to fetch"-feil som tomme tilstander
     */
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
                    val errorMessage = result.exceptionOrNull()?.message ?: ""
                    // Behandle 500-feil eller "Failed to fetch"-feil som tomme tilstander
                    if (errorMessage.contains("500") || errorMessage.contains("Failed to fetch club requests")) {
                        uiState.copy(
                            clubRequests = emptyList(),
                            isLoading = false,
                            error = null
                        )
                    } else {
                        uiState.copy(
                            isLoading = false,
                            error = errorMessage.ifEmpty { "Failed to load club requests" }
                        )
                    }
                }
            }
        }
    }

    /**
     * Godkjenner en klubbforespørsel
     * Oppretter klubben og tildeler bruker som sendte forespørselen til klubbadmin
     * @param id ID-en til klubbforespørselen som skal godkjennes
     * @param onSuccess Callback som kalles ved vellykket godkjenning
     * @param onError Callback som kalles ved feil, med feilmeldingen som parameter
     */
    fun approveClubRequest(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = adminRepository.approveClubRequest(id)

            when {
                result.isSuccess -> {
                    // Last inn klubbforespørsler på nytt etter vellykket godkjenning
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

    /**
     * Avviser en klubbforespørsel
     * Sletter forespørselen fra databasen
     * @param id ID-en til klubbforespørselen som skal avvises
     * @param onSuccess Callback som kalles ved vellykket avvisning
     * @param onError Callback som kalles ved feil, med feilmeldingen som parameter
     */
    fun rejectClubRequest(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = adminRepository.rejectClubRequest(id)

            when {
                result.isSuccess -> {
                    // Last inn klubbforespørsler på nytt etter vellykket avvisning
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

/**
 * UI-tilstand for klubbforespørsel-admin-skjermen
 * @param clubRequests Liste over ventende klubbforespørsler
 * @param isLoading Indikerer om data lastes
 * @param error Feilmelding hvis noe gikk galt
 * @param authError Indikerer om feilen er relatert til autentisering
 */
data class ClubRequestAdminUiState(
    val clubRequests: List<ClubRequestDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val authError: Boolean = false
)

