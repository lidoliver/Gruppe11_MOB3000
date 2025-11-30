package my.application.ui.screen.friends

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import my.application.data.friends.Friend
import my.application.data.repository.FriendsRepository
import my.application.data.repository.UserRepository

/**
 * ViewModel for Friends Screen
 * Manages friends data and state
 */
class FriendsViewModel(application: Application) : AndroidViewModel(application) {

    private val friendsRepository = FriendsRepository.getInstance()
    private val userRepository = UserRepository.getInstance()

    // Liste over venner
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    // Antall ventende venneforespørsler
    private val _friendRequestsCount = MutableStateFlow(0)
    val friendRequestsCount: StateFlow<Int> = _friendRequestsCount.asStateFlow()

    // Lastestatus
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Feilmelding
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Autentiseringsstatus
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        checkAuthentication()
        if (_isAuthenticated.value) {
            loadFriends()
            loadFriendRequestsCount()
        }
    }

    /**
     * Sjekk om brukeren er autentisert
     */
    private fun checkAuthentication() {
        _isAuthenticated.value = userRepository.getAuthToken() != null
    }

    /**
     * Refresh authentication state (call this when returning to the screen)
     */
    fun refreshAuthenticationState() {
        val wasAuthenticated = _isAuthenticated.value
        checkAuthentication()

        // If user just logged in, load friends
        if (!wasAuthenticated && _isAuthenticated.value) {
            loadFriends()
            loadFriendRequestsCount()
        }
    }

    /**
     * Load friends from repository
     * @param showLoading Whether to show loading indicator (default true for initial load, false for background refresh)
     */
    fun loadFriends(showLoading: Boolean = true) {
        Log.d("FriendsViewModel", "loadFriends() called with showLoading=$showLoading")

        // Check authentication before loading
        if (!_isAuthenticated.value) {
            Log.d("FriendsViewModel", "User not authenticated, skipping loadFriends")
            return
        }

        viewModelScope.launch {
            if (showLoading) {
                Log.d("FriendsViewModel", "Setting loading state to true")
                _isLoading.value = true
            }
            _error.value = null

            Log.d("FriendsViewModel", "Calling friendsRepository.getFriends()")
            friendsRepository.getFriends()
                .onSuccess { friendsList ->
                    Log.d("FriendsViewModel", "Success! Received ${friendsList.size} friends")
                    _friends.value = friendsList
                    if (showLoading) {
                        _isLoading.value = false
                    }
                    Log.d("FriendsViewModel", "Updated friends state flow")
                }
                .onFailure { exception ->
                    Log.e("FriendsViewModel", "Failed to load friends: ${exception.message}", exception)
                    _error.value = exception.message ?: "Failed to load friends"
                    if (showLoading) {
                        _isLoading.value = false
                    }
                }
        }
    }

    /**
     * Load friend requests count
     */
    fun loadFriendRequestsCount() {
        Log.d("FriendsViewModel", "loadFriendRequestsCount() called")
        viewModelScope.launch {
            friendsRepository.getFriendRequests()
                .onSuccess { requestsList ->
                    Log.d("FriendsViewModel", "Success! Received ${requestsList.size} friend requests")
                    _friendRequestsCount.value = requestsList.size
                }
                .onFailure { exception ->
                    Log.e("FriendsViewModel", "Failed to load friend requests count: ${exception.message}", exception)
                    _friendRequestsCount.value = 0
                }
        }
    }


    /**
     * Send friend request
     */
    fun sendFriendRequest(friendEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            friendsRepository.sendFriendRequest(friendEmail)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { exception ->
                    onError(exception.message ?: "Failed to send friend request")
                }
        }
    }

    /**
     * Fjern en venn
     */
    fun removeFriend(friendEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            friendsRepository.removeFriend(friendEmail)
                .onSuccess {
                    loadFriends() // Last inn vennelisten på nytt
                    onSuccess()
                }
                .onFailure { exception ->
                    onError(exception.message ?: "Failed to remove friend")
                }
        }
    }

    /**
     * Nullstill feilmelding
     */
    fun clearError() {
        _error.value = null
    }
}

