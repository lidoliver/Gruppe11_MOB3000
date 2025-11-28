package my.application.ui.screen.coursedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import my.application.data.remote.api.PlayerApi
import my.application.data.remote.dto.CourseGameHistory
import android.util.Log

sealed class CourseDetailUiState {
    object Loading : CourseDetailUiState()
    data class Success(val courseHistory: CourseGameHistory) : CourseDetailUiState()
    data class Error(val message: String) : CourseDetailUiState()
}

class CourseDetailViewModel(
    private val api: PlayerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<CourseDetailUiState>(CourseDetailUiState.Loading)
    val uiState: StateFlow<CourseDetailUiState> = _uiState

    fun loadCourseHistory(email: String, courseName: String) {
        Log.d("CourseDetailVM", "Laster historikk for $courseName, spiller: $email")
        _uiState.value = CourseDetailUiState.Loading
        viewModelScope.launch {
            try {
                val history = api.getPlayerCourseGames(email, courseName)
                Log.d("CourseDetailVM", "==========================================")
                Log.d("CourseDetailVM", "API RESPONSE - Total games: ${history.totalGames}")
                history.games.forEach { game ->
                    Log.d("CourseDetailVM", "Game ${game.gameId} playedAt: '${game.playedAt}'")
                }
                Log.d("CourseDetailVM", "==========================================")
                _uiState.value = CourseDetailUiState.Success(history)
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "Feil ved innlasting av banehistorikk", e)
                val errorMessage = when {
                    e.message?.contains("Klarer ikke å resolve host") == true ->
                        "Kan ikke koble til server. Sjekk at backend kjører."
                    e.message?.contains("timeout") == true ->
                        "Serveren svarer ikke. Prøv igjen."
                    e.message?.contains("500") == true ->
                        "Server feil. Sjekk backend logs."
                    else -> e.message ?: "Ukjent feil"
                }
                _uiState.value = CourseDetailUiState.Error(errorMessage)
            }
        }
    }
}

