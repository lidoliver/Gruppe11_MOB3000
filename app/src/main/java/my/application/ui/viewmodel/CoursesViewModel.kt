/**
 * ViewModel for håndtering av baner/courses
 * Håndterer lasting av baner fra repository og autentiseringstilstand
 */
package my.application.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import my.application.data.remote.dto.TrackDto
import my.application.data.repository.TrackRepository
import my.application.data.repository.UserRepository

class CoursesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrackRepository(application.applicationContext)
    private val userRepository = UserRepository.getInstance()

    // Liste over baner
    private val _courses = MutableStateFlow<List<TrackDto>>(emptyList())
    val courses: StateFlow<List<TrackDto>> = _courses

    // Lastestatus
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Feilmelding
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Autentiseringsstatus
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    companion object {
        private const val TAG = "CoursesViewModel"
    }

    init {
        checkAuthentication()
    }

    /**
     * Sjekk om bruker er autentisert
     */
    private fun checkAuthentication() {
        _isAuthenticated.value = userRepository.getAuthToken() != null
    }

    /**
     * Oppdater autentiseringstilstand (kall denne når du kommer tilbake til skjermen)
     */
    fun refreshAuthenticationState() {
        checkAuthentication()
    }

    fun loadCourses() {
        viewModelScope.launch {
            Log.d(TAG, "Starting to load courses...")
            _isLoading.value = true
            _error.value = null
            try {
                repository.getAllTracks()
                    .onSuccess { tracks ->
                        Log.d(TAG, "Successfully loaded ${tracks.size} courses")
                        _courses.value = tracks
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to load courses: ${exception.message}", exception)
                        _error.value = exception.message ?: "Unknown error occurred"
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during loadCourses: ${e.message}", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Finished loading courses. isLoading=${_isLoading.value}, error=${_error.value}, courses size=${_courses.value.size}")
            }
        }
    }
}
