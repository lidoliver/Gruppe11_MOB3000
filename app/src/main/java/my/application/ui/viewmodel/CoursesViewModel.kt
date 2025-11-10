package my.application.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import my.application.data.remote.dto.TrackDto
import my.application.data.repository.TrackRepository

class CoursesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrackRepository(application.applicationContext)

    private val _courses = MutableStateFlow<List<TrackDto>>(emptyList())
    val courses: StateFlow<List<TrackDto>> = _courses

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getAllTracks()
                .onSuccess { tracks ->
                    _courses.value = tracks
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}
