package my.application.ui.screen.friends

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import my.application.data.remote.dto.UserDto
import my.application.data.repository.FriendsRepository

/**
 * ViewModel for vennesøk-skjermen
 * Håndterer brukersøk og venneforespørsler
 */
class FriendSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val friendsRepository = FriendsRepository.getInstance()

    // Søkestreng
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Søkeresultater
    private val _searchResults = MutableStateFlow<List<UserDto>>(emptyList())
    val searchResults: StateFlow<List<UserDto>> = _searchResults.asStateFlow()

    // Lastestatus
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Feilmelding
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Sett med e-postadresser forespørsler er sendt til
    private val _requestSentTo = MutableStateFlow<Set<String>>(emptySet())
    val requestSentTo: StateFlow<Set<String>> = _requestSentTo.asStateFlow()

    /**
     * Oppdater søkestrengen
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            searchUsers(query)
        } else {
            _searchResults.value = emptyList()
        }
    }

    /**
     * Search for users
     */
    private fun searchUsers(query: String) {
        Log.d("FriendSearchViewModel", "searchUsers() called with query: '$query'")
        viewModelScope.launch {
            Log.d("FriendSearchViewModel", "Setting searching state to true")
            _isSearching.value = true
            _error.value = null

            Log.d("FriendSearchViewModel", "Calling friendsRepository.searchUsers()")
            friendsRepository.searchUsers(query)
                .onSuccess { users ->
                    Log.d("FriendSearchViewModel", "Success! Received ${users.size} users")
                    _searchResults.value = users
                    _isSearching.value = false
                    Log.d("FriendSearchViewModel", "Updated search results state flow")
                }
                .onFailure { exception ->
                    Log.e("FriendSearchViewModel", "Failed to search users: ${exception.message}", exception)
                    _error.value = exception.message ?: "Failed to search users"
                    _isSearching.value = false
                    _searchResults.value = emptyList()
                }
        }
    }

    /**
     * Send friend request to a user
     */
    fun sendFriendRequest(userEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            friendsRepository.sendFriendRequest(userEmail)
                .onSuccess {
                    // Mark this user as having a sent request
                    _requestSentTo.value = _requestSentTo.value + userEmail
                    onSuccess()
                }
                .onFailure { exception ->
                    onError(exception.message ?: "Failed to send friend request")
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

