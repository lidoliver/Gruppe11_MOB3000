package my.application.ui.screen.messages

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import my.application.data.messaging.ChatMessage
import my.application.data.messaging.MessagingRepository

class ConversationViewModel(
    private val contactEmail: String
) : ViewModel() {
    private val repository = MessagingRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    init {
        loadMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.getMessages(contactEmail).collect { messages ->
                    _messages.value = messages
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("ConversationViewModel", "Error loading messages", e)
                _error.value = e.message
                _messages.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true
            _error.value = null

            repository.sendMessage(contactEmail, text)
                .onSuccess { newMessage ->
                    // Add the message optimistically to the list
                    _messages.value = _messages.value + newMessage
                    // Reload messages to get the actual saved message
                    loadMessages()
                }
                .onFailure { exception ->
                    Log.e("ConversationViewModel", "Error sending message", exception)
                    _error.value = exception.message
                }

            _isSending.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

