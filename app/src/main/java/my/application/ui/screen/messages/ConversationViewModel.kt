package my.application.ui.screen.messages

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import my.application.data.messaging.ChatMessage
import my.application.data.messaging.PollingMessagingRepository

class ConversationViewModel(
    private val contactEmail: String
) : ViewModel() {
    private val repository = PollingMessagingRepository()

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
                    Log.d("ConversationViewModel", "📩 Received ${messages.size} messages for $contactEmail")
                    _messages.value = messages
                    if (_isLoading.value) _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("ConversationViewModel", "❌ Error loading messages", e)
                _error.value = e.message ?: "Failed to load messages"
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

            Log.d("ConversationViewModel", "📤 Sending message to $contactEmail: $text")

            repository.sendMessage(contactEmail, text)
                .onSuccess { newMessage ->
                    Log.d("ConversationViewModel", "✅ Message sent successfully!")
                    // Polling will automatically pick up the new message
                }
                .onFailure { exception ->
                    Log.e("ConversationViewModel", "❌ Error sending message", exception)
                    _error.value = exception.message ?: "Failed to send message"
                }

            _isSending.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

