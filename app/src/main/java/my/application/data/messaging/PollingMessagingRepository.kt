package my.application.data.messaging

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import my.application.data.repository.MessagesRepository
import my.application.data.repository.UserRepository
/**
 * Simple polling-based messaging repository
 * Automatically refreshes messages every few seconds
 */
class PollingMessagingRepository(
    private val messagesRepository: MessagesRepository = MessagesRepository(),
    private val userRepository: UserRepository = UserRepository.getInstance()
) {
    private val TAG = "PollingMessagingRepo"
    private val POLL_INTERVAL_MS = 2000L // Poll every 2 seconds

    /**
     * Get messages with automatic polling
     */
    fun getMessages(contactEmail: String): Flow<List<ChatMessage>> = flow {
        val currentUserEmail = userRepository.getUserEmail()
        val token = userRepository.getAuthToken()

        if (token == null || currentUserEmail == null) {
            Log.e(TAG, "No auth token or user email")
            emit(emptyList())
            return@flow
        }

        val cleanToken = token.removePrefix("Bearer ")

        // Poll for messages continuously
        while (true) {
            try {
                Log.d(TAG, "Polling messages for $contactEmail...")
                messagesRepository.getMessages(contactEmail, cleanToken)
                    .onSuccess { messagesDto ->
                        val chatMessages = messagesDto.map { dto ->
                            ChatMessage(
                                id = dto.messageId,
                                conversationId = contactEmail.hashCode(),
                                senderId = dto.senderEmail.hashCode(),
                                text = dto.content,
                                timestamp = formatTimestamp(dto.createdAt),
                                isFromMe = dto.senderEmail == currentUserEmail,
                                isRead = dto.isRead
                            )
                        }
                        Log.d(TAG, "Emitting ${chatMessages.size} messages")
                        emit(chatMessages)
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Error fetching messages", exception)
                    }

                // Wait before next poll
                delay(POLL_INTERVAL_MS)
            } catch (e: Exception) {
                Log.e(TAG, "Polling error", e)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Send a message
     */
    suspend fun sendMessage(contactEmail: String, text: String): Result<ChatMessage> {
        val token = userRepository.getAuthToken()
        val currentUserEmail = userRepository.getUserEmail()

        if (token == null || currentUserEmail == null) {
            return Result.failure(Exception("Not authenticated"))
        }

        val cleanToken = token.removePrefix("Bearer ")

        return messagesRepository.sendMessage(contactEmail, text, cleanToken)
            .map {
                // Opprett midlertidig melding for umiddelbar visning
                ChatMessage(
                    id = System.currentTimeMillis().toInt(),
                    conversationId = contactEmail.hashCode(),
                    senderId = currentUserEmail.hashCode(),
                    text = text,
                    timestamp = "Nå",
                    isFromMe = true,
                    isRead = true
                )
            }
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val dateTime = java.time.ZonedDateTime.parse(timestamp)
            val now = java.time.ZonedDateTime.now()

            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate())

            when {
                daysBetween == 0L -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                daysBetween == 1L -> "I går"
                daysBetween < 7 -> "$daysBetween dager siden"
                else -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing timestamp: $timestamp", e)
            timestamp
        }
    }
}

