package my.application.ui.screen.friends

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import my.application.data.remote.dto.FriendRequestDto
import my.application.data.repository.FriendsRepository

/**
 * ViewModel for Friend Requests Screen
 * Manages incoming friend requests that user can accept or decline
 */
class FriendRequestsViewModel(application: Application) : AndroidViewModel(application) {

    private val friendsRepository = FriendsRepository.getInstance()

    private val _friendRequests = MutableStateFlow<List<FriendRequestDto>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequestDto>> = _friendRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadFriendRequests()
    }

    /**
     * Load friend requests from repository
     */
    fun loadFriendRequests() {
        Log.d("FriendRequestsViewModel", "loadFriendRequests() called")
        viewModelScope.launch {
            Log.d("FriendRequestsViewModel", "Setting loading state to true")
            _isLoading.value = true
            _error.value = null

            Log.d("FriendRequestsViewModel", "Calling friendsRepository.getFriendRequests()")
            friendsRepository.getFriendRequests()
                .onSuccess { requestsList ->
                    Log.d("FriendRequestsViewModel", "Success! Received ${requestsList.size} friend requests")
                    _friendRequests.value = requestsList
                    _isLoading.value = false
                    Log.d("FriendRequestsViewModel", "Updated friend requests state flow")
                }
                .onFailure { exception ->
                    Log.e("FriendRequestsViewModel", "Failed to load friend requests: ${exception.message}", exception)
                    _error.value = exception.message ?: "Failed to load friend requests"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Accept a friend request
     */
    fun acceptFriendRequest(requestId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Log.d("FriendRequestsViewModel", "acceptFriendRequest() called for request $requestId")
        viewModelScope.launch {
            friendsRepository.acceptFriendRequest(requestId)
                .onSuccess {
                    Log.d("FriendRequestsViewModel", "Successfully accepted request $requestId")
                    loadFriendRequests() // Reload the list
                    onSuccess()
                }
                .onFailure { exception ->
                    Log.e("FriendRequestsViewModel", "Failed to accept request: ${exception.message}", exception)
                    onError(exception.message ?: "Failed to accept friend request")
                }
        }
    }

    /**
     * Decline a friend request
     */
    fun declineFriendRequest(requestId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Log.d("FriendRequestsViewModel", "declineFriendRequest() called for request $requestId")
        viewModelScope.launch {
            friendsRepository.declineFriendRequest(requestId)
                .onSuccess {
                    Log.d("FriendRequestsViewModel", "Successfully declined request $requestId")
                    loadFriendRequests() // Reload the list
                    onSuccess()
                }
                .onFailure { exception ->
                    Log.e("FriendRequestsViewModel", "Failed to decline request: ${exception.message}", exception)
                    onError(exception.message ?: "Failed to decline friend request")
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

