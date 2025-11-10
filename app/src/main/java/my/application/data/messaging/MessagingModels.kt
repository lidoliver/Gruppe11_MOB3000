package my.application.data.messaging

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import my.application.data.remote.dto.ConversationDto
import my.application.data.remote.dto.MessageDto
import my.application.data.repository.MessagesRepository
import my.application.data.repository.UserRepository
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Data models for messaging feature
 * Populated from the database via backend API
 */

/**
 * Represents a conversation/contact in the message list
 */
data class Conversation(
    val id: Int,
    val contactId: Int,
    val contactEmail: String,
    val contactName: String,
    val lastMessagePreview: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val avatarColor: Long
)

/**
 * Represents an individual message in a conversation
 */
data class ChatMessage(
    val id: Int,
    val conversationId: Int,
    val senderId: Int,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val isRead: Boolean = true
)

/**
 * Repository for messaging with database integration
 */
class MessagingRepository(
    private val messagesRepository: MessagesRepository = MessagesRepository(),
    private val userRepository: UserRepository = UserRepository.getInstance()
) {

    // Simple encryption key - In production, use Android Keystore
    private val ENCRYPTION_KEY = "MySecureKey12345" // 16 chars for AES-128

    /**
     * Get all conversations for the current user
     */
    fun getConversations(): Flow<List<Conversation>> = flow {
        val token = userRepository.getAuthToken()
        if (token == null) {
            Log.e("MessagingRepository", "No auth token available")
            emit(emptyList())
            return@flow
        }

        val cleanToken = token.removePrefix("Bearer ")
        messagesRepository.getConversations(cleanToken)
            .onSuccess { conversationsDto ->
                emit(conversationsDto.map { it.toConversation() })
            }
            .onFailure { exception ->
                Log.e("MessagingRepository", "Error loading conversations", exception)
                emit(emptyList())
            }
    }

    /**
     * Get all messages for a specific conversation by contact email
     */
    fun getMessages(contactEmail: String): Flow<List<ChatMessage>> = flow {
        val token = userRepository.getAuthToken()
        val currentUserEmail = userRepository.getUserEmail()

        Log.d("MessagingRepository", "Loading messages for $contactEmail")
        Log.d("MessagingRepository", "Auth token available: ${token != null}, User email: $currentUserEmail")

        if (token == null || currentUserEmail == null) {
            Log.e("MessagingRepository", "No auth token or user email available")
            emit(emptyList())
            return@flow
        }

        val cleanToken = token.removePrefix("Bearer ")
        messagesRepository.getMessages(contactEmail, cleanToken)
            .onSuccess { messagesDto ->
                Log.d("MessagingRepository", "Received ${messagesDto.size} messages from API")
                val chatMessages = messagesDto.map { it.toChatMessage(currentUserEmail, contactEmail) }
                Log.d("MessagingRepository", "Emitting ${chatMessages.size} chat messages")
                emit(chatMessages)
            }
            .onFailure { exception ->
                Log.e("MessagingRepository", "Error loading messages", exception)
                emit(emptyList())
            }
    }

    /**
     * Send a message to a contact
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
                // Note: Backend doesn't return full message, so we create a temporary one
                ChatMessage(
                    id = 0, // Will be replaced on refresh
                    conversationId = contactEmail.hashCode(),
                    senderId = currentUserEmail.hashCode(),
                    text = text,
                    timestamp = ZonedDateTime.now().toString(),
                    isFromMe = true,
                    isRead = true
                )
            }
    }

    /**
     * Mark messages as read (handled automatically by backend when fetching messages)
     */
    suspend fun markMessagesAsRead(contactEmail: String): Result<Unit> {
        val token = userRepository.getAuthToken()
        if (token == null) {
            return Result.failure(Exception("Not authenticated"))
        }

        val cleanToken = token.removePrefix("Bearer ")
        return messagesRepository.getMessages(contactEmail, cleanToken)
            .map { Unit }
    }

    /**
     * Encrypt a message (placeholder for future end-to-end encryption)
     * Currently returns plain text as backend doesn't support encryption yet
     */
    fun encryptMessage(text: String): String {
        return try {
            val key = SecretKeySpec(ENCRYPTION_KEY.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(text.toByteArray())
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            Log.e("MessagingRepository", "Encryption error, returning plain text", e)
            text // Fallback to plain text
        }
    }

    /**
     * Decrypt a message (placeholder for future end-to-end encryption)
     * Currently returns plain text as backend doesn't support encryption yet
     */
    fun decryptMessage(encryptedText: String): String {
        return try {
            val key = SecretKeySpec(ENCRYPTION_KEY.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decoded = Base64.getDecoder().decode(encryptedText)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted)
        } catch (e: Exception) {
            Log.e("MessagingRepository", "Decryption error, returning as-is", e)
            encryptedText // Fallback to returning as-is
        }
    }

    // Conversion functions

    private fun ConversationDto.toConversation(): Conversation {
        return Conversation(
            id = contactEmail.hashCode(),
            contactId = contactEmail.hashCode(),
            contactEmail = contactEmail,
            contactName = contactUsername,
            lastMessagePreview = lastMessage ?: "No messages yet",
            timestamp = formatTimestamp(lastMessageTime),
            isRead = unreadCount == 0,
            avatarColor = generateColorFromEmail(contactEmail)
        )
    }

    private fun MessageDto.toChatMessage(currentUserEmail: String, contactEmail: String): ChatMessage {
        return ChatMessage(
            id = messageId,
            conversationId = contactEmail.hashCode(),
            senderId = senderEmail.hashCode(),
            text = content,
            timestamp = formatTimestamp(createdAt),
            isFromMe = senderEmail == currentUserEmail,
            isRead = isRead
        )
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val dateTime = ZonedDateTime.parse(timestamp)
            val now = ZonedDateTime.now()

            val daysBetween = ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate())

            when {
                daysBetween == 0L -> dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                daysBetween == 1L -> "I går"
                daysBetween < 7 -> "$daysBetween dager siden"
                else -> dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }
        } catch (e: Exception) {
            Log.e("MessagingRepository", "Error parsing timestamp: $timestamp", e)
            timestamp
        }
    }

    private fun generateColorFromEmail(email: String): Long {
        val colors = listOf(
            0xFF6750A4,
            0xFF625B71,
            0xFF7D5260,
            0xFF006874,
            0xFF8E4585,
            0xFF4A5E3F,
            0xFF7C4DFF,
            0xFF00BFA5,
            0xFFFF6F00
        )
        val hash = email.hashCode()
        return colors[(hash.absoluteValue % colors.size)]
    }

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this
}

