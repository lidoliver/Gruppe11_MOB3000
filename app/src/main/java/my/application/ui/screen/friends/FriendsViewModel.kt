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

/**
 * ViewModel for Friends Screen
 * Manages friends data and state
 */
class FriendsViewModel(application: Application) : AndroidViewModel(application) {

    private val friendsRepository = FriendsRepository.getInstance()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _friendRequestsCount = MutableStateFlow(0)
    val friendRequestsCount: StateFlow<Int> = _friendRequestsCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadFriends()
        loadFriendRequestsCount()
    }

    /**
     * Load friends from repository
     */
    fun loadFriends() {
        Log.d("FriendsViewModel", "loadFriends() called")
        viewModelScope.launch {
            Log.d("FriendsViewModel", "Setting loading state to true")
            _isLoading.value = true
            _error.value = null

            Log.d("FriendsViewModel", "Calling friendsRepository.getFriends()")
            friendsRepository.getFriends()
                .onSuccess { friendsList ->
                    Log.d("FriendsViewModel", "Success! Received ${friendsList.size} friends")
                    _friends.value = friendsList
                    _isLoading.value = false
                    Log.d("FriendsViewModel", "Updated friends state flow")
                }
                .onFailure { exception ->
                    Log.e("FriendsViewModel", "Failed to load friends: ${exception.message}", exception)
                    _error.value = exception.message ?: "Failed to load friends"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Load friend requests count
     */
    private fun loadFriendRequestsCount() {
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
     * Remove a friend
     */
    fun removeFriend(friendEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            friendsRepository.removeFriend(friendEmail)
                .onSuccess {
                    loadFriends() // Reload friends list
                    onSuccess()
                }
                .onFailure { exception ->
                    onError(exception.message ?: "Failed to remove friend")
                }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}

