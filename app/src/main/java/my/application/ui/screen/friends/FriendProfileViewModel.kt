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
import my.application.data.remote.dto.PlayerStats
import my.application.data.remote.RetrofitProvider
import my.application.data.repository.FriendsRepository

/**
 * ViewModel for Friend Profile Screen
 * Manages friend profile data and statistics
 */
class FriendProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val friendsRepository = FriendsRepository.getInstance()
    private val playerApi = RetrofitProvider.playerApi

    private val _friend = MutableStateFlow<Friend?>(null)
    val friend: StateFlow<Friend?> = _friend.asStateFlow()

    private val _stats = MutableStateFlow<PlayerStats?>(null)
    val stats: StateFlow<PlayerStats?> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Load friend profile and statistics
     */
    fun loadFriendProfile(friendEmail: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Load friends list and find the specific friend
                friendsRepository.getFriends()
                    .onSuccess { friendsList ->
                        val friendData = friendsList.find { it.email == friendEmail }
                        if (friendData != null) {
                            _friend.value = friendData

                            // Load player stats
                            loadPlayerStats(friendEmail)
                        } else {
                            _error.value = "Vennen ble ikke funnet"
                            _isLoading.value = false
                        }
                    }
                    .onFailure { exception ->
                        Log.e("FriendProfileViewModel", "Failed to load friend", exception)
                        _error.value = exception.message ?: "Kunne ikke laste vennprofil"
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("FriendProfileViewModel", "Error loading friend profile", e)
                _error.value = e.message ?: "En feil oppstod"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load player statistics
     */
    private suspend fun loadPlayerStats(email: String) {
        try {
            val statsData = playerApi.getPlayerStats(email)
            _stats.value = statsData
            _isLoading.value = false
        } catch (e: Exception) {
            Log.e("FriendProfileViewModel", "Failed to load player stats", e)
            // Don't set error here, just log it and continue
            // The profile can still be shown without stats
            _stats.value = null
            _isLoading.value = false
        }
    }

    /**
     * Remove friend
     */
    fun removeFriend(friendEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            friendsRepository.removeFriend(friendEmail)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { exception ->
                    Log.e("FriendProfileViewModel", "Failed to remove friend", exception)
                    onError(exception.message ?: "Kunne ikke fjerne venn")
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

