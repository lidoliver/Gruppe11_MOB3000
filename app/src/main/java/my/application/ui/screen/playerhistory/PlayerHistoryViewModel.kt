package my.application.ui.screen.playerhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import my.application.data.remote.api.PlayerApi
import my.application.data.remote.dto.PlayerStats
import android.util.Log

sealed class UiState {
    object Loading : UiState()
    data class Success(val stats: PlayerStats) : UiState()
    data class Error(val message: String) : UiState()
}

class PlayerHistoryViewModel(
    private val api: PlayerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    fun load(email: String) {
        Log.d("PlayerHistoryVM", "Laster inn statistikk for email: $email")
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val stats = api.getPlayerStats(email)
                Log.d("PlayerHistoryVM", "==========================================")
                Log.d("PlayerHistoryVM", "API RESPONSE - lastPlayedAt: '${stats.lastPlayedAt}'")
                Log.d("PlayerHistoryVM", "Full stats: $stats")
                Log.d("PlayerHistoryVM", "==========================================")
                _uiState.value = UiState.Success(stats)
            } catch (e: Exception) {
                Log.e("PlayerHistoryVM", "Feil ved innlasting av statistikk for $email", e)
                val errorMessage = when {
                    e.message?.contains("Klarer ikke å resolve host") == true ->
                        "Kan ikke koble til server. Sjekk at backend kjører."
                    e.message?.contains("timeout") == true ->
                        "Serveren svarer ikke. Prøv igjen."
                    e.message?.contains("500") == true ->
                        "Server feil. Sjekk backend logs."
                    else -> e.message ?: "Ukjent feil"
                }
                _uiState.value = UiState.Error(errorMessage)
            }
        }
    }
}

