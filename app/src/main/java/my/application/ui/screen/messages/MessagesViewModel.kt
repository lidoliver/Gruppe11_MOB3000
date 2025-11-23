package my.application.ui.screen.messages

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import my.application.data.messaging.Conversation
import my.application.data.messaging.FirestoreMessagingRepository

class MessagesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirestoreMessagingRepository()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getConversations().collect { conversations ->
                    _conversations.value = conversations
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Error loading conversations", e)
                _error.value = e.message
                _conversations.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

