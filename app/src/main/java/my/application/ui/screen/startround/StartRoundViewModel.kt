package my.application.ui.screen.startround

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import my.application.data.repository.UserRepository

/**
 * ViewModel for start runde-skjermen
 * Håndterer autentiseringstilstand
 */
class StartRoundViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository.getInstance()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

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
}

