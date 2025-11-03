package my.application.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import My.application.data.remote.dto.CourseDto
import my.application.data.repository.DiscGolfRepository

class CoursesViewModel : ViewModel() {
    private val repository = DiscGolfRepository()

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
            repository.getAllCourses()
                .onSuccess { _courses.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}
