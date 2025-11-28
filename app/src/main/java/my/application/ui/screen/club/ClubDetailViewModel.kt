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
import my.application.data.remote.dto.ClubPageDto
import my.application.data.repository.ClubPageRepository

class ClubDetailViewModel(
    application: Application,
    private val clubId: Int
) : AndroidViewModel(application) {
    private val clubPageRepository: ClubPageRepository = ClubPageRepository.getInstance()
    private val authManager: AuthManager = AuthManager.getInstance(application)

    var uiState by mutableStateOf(ClubDetailUiState())
        private set

    init {
        viewModelScope.launch {
            // Get auth token and set it
            val token = authManager.getAuthToken()
            if (token != null) {
                clubPageRepository.setAuthToken("Bearer $token")
            }
            // Then load club page
            loadClubPage()
        }
    }

    fun loadClubPage() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            val result = clubPageRepository.getClubPage(clubId)
            result.fold(
                onSuccess = { clubPage ->
                    Log.d("ClubDetailViewModel", "Club page loaded")
                    uiState = uiState.copy(isLoading = false, clubPage = clubPage, error = null)
                },
                onFailure = { error ->
                    Log.e("ClubDetailViewModel", "Failed to load club page", error)
                    uiState = uiState.copy(isLoading = false, error = error.message ?: "Failed to load club page")
                }
            )
        }
    }

    fun createAnnouncement(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            uiState = uiState.copy(isCreating = true, error = null)
            val result = clubPageRepository.createAnnouncement(clubId, content)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(isCreating = false)
                    loadClubPage()
                },
                onFailure = { error ->
                    uiState = uiState.copy(isCreating = false, error = "Failed to create announcement")
                }
            )
        }
    }

    fun deleteAnnouncement(announcementId: Int) {
        viewModelScope.launch {
            clubPageRepository.deleteAnnouncement(clubId, announcementId)
            loadClubPage()
        }
    }

    fun createEvent(title: String, description: String?, eventDate: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            Log.d("ClubDetailViewModel", "Creating event: title=$title, description=$description, eventDate=$eventDate, clubId=$clubId")
            uiState = uiState.copy(isCreating = true, error = null)
            val result = clubPageRepository.createEvent(clubId, title, description, eventDate)
            result.fold(
                onSuccess = {
                    Log.d("ClubDetailViewModel", "Event created successfully")
                    uiState = uiState.copy(isCreating = false)
                    loadClubPage()
                },
                onFailure = { error ->
                    Log.e("ClubDetailViewModel", "Failed to create event: ${error.message}", error)
                    uiState = uiState.copy(isCreating = false, error = "Failed to create event: ${error.message}")
                }
            )
        }
    }

    fun deleteEvent(eventId: Int) {
        viewModelScope.launch {
            clubPageRepository.deleteEvent(clubId, eventId)
            loadClubPage()
        }
    }

    fun joinEvent(eventId: Int) {
        viewModelScope.launch {
            clubPageRepository.joinEvent(clubId, eventId)
            loadClubPage()
        }
    }

    fun leaveEvent(eventId: Int) {
        viewModelScope.launch {
            clubPageRepository.leaveEvent(clubId, eventId)
            loadClubPage()
        }
    }
}

data class ClubDetailUiState(
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val clubPage: ClubPageDto? = null,
    val error: String? = null
)

