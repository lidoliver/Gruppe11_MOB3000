package my.application.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import my.application.data.remote.dto.CourseDto
import my.application.data.repository.TrackRepository

class CoursesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrackRepository(application.applicationContext)

    private val _courses = MutableStateFlow<List<CourseDto>>(emptyList())
    val courses: StateFlow<List<CourseDto>> = _courses

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
                    // Convert TrackDto to CourseDto
                    _courses.value = tracks.map { track ->
                        CourseDto(
                            courseId = track.baneId.toString(),
                            courseName = track.name,
                            holeCount = track.holes,
                            city = "Unknown", // Not available in TrackDto
                            isProLevel = false // Not available in TrackDto
                        )
                    }
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}
