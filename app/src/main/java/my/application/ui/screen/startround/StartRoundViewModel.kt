package my.application.ui.screen.startround

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import my.application.data.repository.UserRepository

/**
 * ViewModel for Start Round Screen
 * Manages authentication state
 */
class StartRoundViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository.getInstance()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        checkAuthentication()
    }

    /**
     * Check if user is authenticated
     */
    private fun checkAuthentication() {
        _isAuthenticated.value = userRepository.getAuthToken() != null
    }

    /**
     * Refresh authentication state (call this when returning to the screen)
     */
    fun refreshAuthenticationState() {
        checkAuthentication()
    }
}

