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

class CreateClubViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val clubRepository: ClubRepository = ClubRepository.getInstance()

    private val _uiState = MutableStateFlow(CreateClubUiState())
    val uiState: StateFlow<CreateClubUiState> = _uiState.asStateFlow()

    fun onClubNameChange(name: String) {
        _uiState.value = _uiState.value.copy(
            clubName = name,
            clubNameError = null
        )
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(
            description = description,
            descriptionError = null
        )
    }

    fun submitClubRequest() {
        val currentState = _uiState.value

        // Validation
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

        // Submit request
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

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

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

