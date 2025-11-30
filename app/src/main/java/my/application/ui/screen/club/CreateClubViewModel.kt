package my.application.ui.screen.club

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import my.application.data.repository.ClubRepository

/**
 * ViewModel for opprettelse av nye klubber
 * Håndterer validering og innsending av klubbforespørsler til admin
 */
class CreateClubViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val clubRepository: ClubRepository = ClubRepository.getInstance()

    private val _uiState = MutableStateFlow(CreateClubUiState())
    val uiState: StateFlow<CreateClubUiState> = _uiState.asStateFlow()

    /**
     * Oppdaterer klubbnavnet i UI-tilstanden
     * Fjerner eventuelle feilmeldinger når bruker skriver
     */
    fun onClubNameChange(name: String) {
        _uiState.value = _uiState.value.copy(
            clubName = name,
            clubNameError = null
        )
    }

    /**
     * Oppdaterer beskrivelsen/søknadsbrevet i UI-tilstanden
     * Fjerner eventuelle feilmeldinger når bruker skriver
     */
    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(
            description = description,
            descriptionError = null
        )
    }

    /**
     * Sender inn klubbforespørselen til backend for godkjenning av admin
     * Validerer input før innsending
     */
    fun submitClubRequest() {
        val currentState = _uiState.value

        // Validering av input
        var hasError = false

        if (currentState.clubName.isBlank()) {
            _uiState.value = currentState.copy(
                clubNameError = "Klubbnavn er påkrevd"
            )
            hasError = true
        }

        if (currentState.description.isBlank()) {
            _uiState.value = currentState.copy(
                descriptionError = "Søknadsbrev er påkrevd"
            )
            hasError = true
        } else if (currentState.description.length < 50) {
            _uiState.value = currentState.copy(
                descriptionError = "Søknadsbrev må være minst 50 tegn"
            )
            hasError = true
        }

        if (hasError) return

        // Send forespørsel til backend
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = clubRepository.submitClubRequest(
                clubName = currentState.clubName,
                description = currentState.description
            )

            result.fold(
                onSuccess = { message ->
                    Log.d("CreateClubViewModel", "Club request submitted successfully")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSubmitted = true,
                        successMessage = message
                    )
                },
                onFailure = { error ->
                    Log.e("CreateClubViewModel", "Failed to submit club request", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Kunne ikke sende søknad"
                    )
                }
            )
        }
    }

    /**
     * Tømmer feil- og suksessmeldinger fra UI-tilstanden
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

/**
 * UI-tilstand for klubbopprettelse-skjermen
 * @param clubName Navnet på klubben som skal opprettes
 * @param description Beskrivelse/søknadsbrev for klubben
 * @param clubNameError Feilmelding for klubbnavn-feltet
 * @param descriptionError Feilmelding for beskrivelse-feltet
 * @param isLoading Indikerer om forespørselen er under behandling
 * @param isSubmitted Indikerer om forespørselen er sendt inn
 * @param errorMessage Generell feilmelding
 * @param successMessage Suksessmelding
 */
data class CreateClubUiState(
    val clubName: String = "",
    val description: String = "",
    val clubNameError: String? = null,
    val descriptionError: String? = null,
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

