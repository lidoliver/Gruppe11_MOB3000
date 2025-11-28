package my.application.data.messaging

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import my.application.data.repository.MessagesRepository
import my.application.data.repository.UserRepository

/**
 * Firebase Firestore-based messaging repository with real-time updates
 * Uses Firestore for READING (real-time listeners) and Backend API for WRITING (notifications)
 */
class FirestoreMessagingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val messagesRepository: MessagesRepository = MessagesRepository(),
    private val userRepository: UserRepository = UserRepository.getInstance()
) {
    private val TAG = "FirestoreMessaging"

    /**
     * Get all conversations for the current user with real-time updates
     */
    fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        val currentUserEmail = userRepository.getUserEmail()
        if (currentUserEmail == null) {
            Log.e(TAG, "No user email available")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d(TAG, "Setting up conversations listener for: $currentUserEmail")

        // Listen to conversations collection for this user
        val listener = firestore
            .collection("conversations")
            .whereArrayContains("participants", currentUserEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to conversations", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d(TAG, "No conversations found")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val conversations = snapshot.documents.mapNotNull { doc ->
                    try {
                        val participants = doc.get("participants") as? List<String> ?: return@mapNotNull null
                        val contactEmail = participants.firstOrNull { it != currentUserEmail } ?: return@mapNotNull null

                        val lastMessage = doc.getString("lastMessage") ?: "No messages yet"
                        val lastMessageTime = doc.get("lastMessageTime") as? com.google.firebase.Timestamp
                        val unreadCount = (doc.getLong("unreadCount_$currentUserEmail") ?: 0).toInt()
                        val contactName = doc.getString("contactName_$contactEmail") ?: contactEmail.split("@")[0]

                        Conversation(
                            id = doc.id.hashCode(),
                            contactId = contactEmail.hashCode(),
                            contactEmail = contactEmail,
                            contactName = contactName,
                            lastMessagePreview = lastMessage,
                            timestamp = formatTimestamp(lastMessageTime?.toDate()?.time ?: System.currentTimeMillis()),
                            isRead = unreadCount == 0,
                            avatarColor = generateColorFromEmail(contactEmail)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing conversation", e)
                        null
                    }
                }

                Log.d(TAG, "Emitting ${conversations.size} conversations")
                trySend(conversations.sortedByDescending { it.timestamp })
            }

        awaitClose {
            Log.d(TAG, "Removing conversations listener")
            listener.remove()
        }
    }

    /**
     * Get messages for a specific conversation with real-time updates
     */
    fun getMessages(contactEmail: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserEmail = userRepository.getUserEmail()
        if (currentUserEmail == null) {
            Log.e(TAG, "No user email available")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val conversationId = generateConversationId(currentUserEmail, contactEmail)
        Log.d(TAG, "Setting up messages listener for conversation: $conversationId")

        // Listen to messages in this conversation
        val listener = firestore
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.d(TAG, "No messages yet")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        val senderEmail = doc.getString("senderEmail") ?: return@mapNotNull null
                        val content = doc.getString("content") ?: return@mapNotNull null
                        val timestamp = doc.get("timestamp") as? com.google.firebase.Timestamp
                        val isRead = doc.getBoolean("isRead") ?: false

                        ChatMessage(
                            id = doc.id.hashCode(),
                            conversationId = conversationId.hashCode(),
                            senderId = senderEmail.hashCode(),
                            text = content,
                            timestamp = formatTimestamp(timestamp?.toDate()?.time ?: System.currentTimeMillis()),
                            isFromMe = senderEmail == currentUserEmail,
                            isRead = isRead
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                        null
                    }
                }

                Log.d(TAG, "Emitting ${messages.size} messages")
                trySend(messages)
            }

        awaitClose {
            Log.d(TAG, "Removing messages listener")
            listener.remove()
        }
    }

    /**
     * Send a message to a contact
     * Uses backend API to ensure notifications are sent and dual-write happens
     */
    suspend fun sendMessage(contactEmail: String, text: String): Result<ChatMessage> {
        return try {
            val token = userRepository.getAuthToken()
            val currentUserEmail = userRepository.getUserEmail()

            if (token == null || currentUserEmail == null) {
                return Result.failure(Exception("Not authenticated"))
            }

            val cleanToken = token.removePrefix("Bearer ")

            Log.d(TAG, "Sending message via backend API to $contactEmail")

            // Send via backend API - this triggers:
            // 1. PostgreSQL write (for backup/analytics)
            // 2. Firestore write (for real-time sync via backend)
            // 3. Push notification to receiver
            messagesRepository.sendMessage(contactEmail, text, cleanToken)
                .map {
                    Log.d(TAG, "✅ Message sent, notification triggered")
                    // Create message for immediate display
                    ChatMessage(
                        id = System.currentTimeMillis().toInt(),
                        conversationId = generateConversationId(currentUserEmail, contactEmail).hashCode(),
                        senderId = currentUserEmail.hashCode(),
                        text = text,
                        timestamp = "Nå",
                        isFromMe = true,
                        isRead = false
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to send message: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }


    /**
     * Generate a consistent conversation ID for two users
     */
    private fun generateConversationId(email1: String, email2: String): String {
        val sorted = listOf(email1, email2).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    /**
     * Format timestamp for display
     */
    private fun formatTimestamp(timestampMillis: Long): String {
        return try {
            val dateTime = java.time.Instant.ofEpochMilli(timestampMillis)
                .atZone(java.time.ZoneId.systemDefault())
            val now = java.time.ZonedDateTime.now()

            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                dateTime.toLocalDate(),
                now.toLocalDate()
            )

            when {
                daysBetween == 0L -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                daysBetween == 1L -> "I går"
                daysBetween < 7 -> "$daysBetween dager siden"
                else -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting timestamp", e)
            "Nå"
        }
    }

    /**
     * Generate avatar color from email
     */
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
        val absoluteHash = if (hash < 0) -hash else hash
        return colors[absoluteHash % colors.size]
    }
}

