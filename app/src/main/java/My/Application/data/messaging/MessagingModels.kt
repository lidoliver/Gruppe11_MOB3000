package My.Application.data.messaging

/**
 * Data models for messaging feature
 * These models mirror the database structure and will be used for both demo and real data
 */

/**
 * Represents a conversation/contact in the message list
 * This will be populated from the database in the future
 */
data class Conversation(
    val id: Int,
    val contactId: Int,
    val contactName: String,
    val lastMessagePreview: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val avatarColor: Long // Stored as Long for database compatibility (will use Color(avatarColor) in UI)
)

/**
 * Represents an individual message in a conversation
 * This will be populated from the database in the future
 */
data class ChatMessage(
    val id: Int,
    val conversationId: Int,
    val senderId: Int,
    val text: String, // Will be encrypted in database
    val timestamp: String,
    val isFromMe: Boolean,
    val isRead: Boolean = true
)

/**
 * Repository for demo data
 * TODO: Replace with actual database repository when backend is ready
 */
object MessagingDemoRepository {

    /**
     * Demo conversations - Replace with database query
     * Future: SELECT * FROM conversations WHERE user_id = current_user ORDER BY timestamp DESC
     */
    fun getDemoConversations(): List<Conversation> {
        return listOf(
            Conversation(
                id = 1,
                contactId = 101,
                contactName = "Erik Hansen",
                lastMessagePreview = "Hei! Skal du spille i helgen?",
                timestamp = "10:30",
                isRead = false,
                avatarColor = 0xFF6750A4
            ),
            Conversation(
                id = 2,
                contactId = 102,
                contactName = "Maria Johansen",
                lastMessagePreview = "Gratulerer med ny rekord!",
                timestamp = "I går",
                isRead = true,
                avatarColor = 0xFF625B71
            ),
            Conversation(
                id = 3,
                contactId = 103,
                contactName = "Lars Olsen",
                lastMessagePreview = "Møtes vi på Ekeberg bane kl 14?",
                timestamp = "I går",
                isRead = true,
                avatarColor = 0xFF7D5260
            ),
            Conversation(
                id = 4,
                contactId = 104,
                contactName = "Disc Golf Klubben",
                lastMessagePreview = "Nytt arrangement neste onsdag!",
                timestamp = "2 dager siden",
                isRead = true,
                avatarColor = 0xFF006874
            ),
            Conversation(
                id = 5,
                contactId = 105,
                contactName = "Sofie Berg",
                lastMessagePreview = "Takk for sist! Det var gøy 🥏",
                timestamp = "3 dager siden",
                isRead = true,
                avatarColor = 0xFF8E4585
            )
        )
    }

    /**
     * Demo messages for a specific conversation - Replace with database query
     * Future: SELECT * FROM messages WHERE conversation_id = ? ORDER BY timestamp ASC
     * Note: Messages will be encrypted in database and decrypted here
     */
    fun getDemoMessages(conversationId: Int, currentUserId: Int = 1): List<ChatMessage> {
        return when (conversationId) {
            1 -> listOf(
                ChatMessage(1, 1, 101, "Hei! Skal du spille i helgen?", "10:25", false),
                ChatMessage(2, 1, currentUserId, "Ja, jeg tenkte å dra på lørdag!", "10:30", true),
                ChatMessage(3, 1, 101, "Perfekt! Ekeberg bane?", "10:31", false),
                ChatMessage(4, 1, currentUserId, "Høres bra ut! Klokken 14?", "10:32", true),
                ChatMessage(5, 1, 101, "Deal! Ses der 🥏", "10:33", false)
            )
            2 -> listOf(
                ChatMessage(6, 2, 102, "Gratulerer med ny rekord!", "09:15", false),
                ChatMessage(7, 2, currentUserId, "Takk skal du ha! 😊", "09:20", true),
                ChatMessage(8, 2, 102, "Du må lære meg dine triks!", "09:21", false),
                ChatMessage(9, 2, currentUserId, "Selvfølgelig! Vi kan øve sammen neste uke", "09:25", true)
            )
            3 -> listOf(
                ChatMessage(10, 3, 103, "Møtes vi på Ekeberg bane kl 14?", "08:45", false),
                ChatMessage(11, 3, currentUserId, "Ja, det passer fint!", "09:00", true),
                ChatMessage(12, 3, 103, "Supert! Tar med ekstra disker", "09:05", false)
            )
            4 -> listOf(
                ChatMessage(13, 4, 104, "Nytt arrangement neste onsdag!", "12:00", false),
                ChatMessage(14, 4, currentUserId, "Hva er tidspunktet?", "12:30", true),
                ChatMessage(15, 4, 104, "Vi starter kl 18:00. Påmelding senest mandag.", "12:35", false),
                ChatMessage(16, 4, currentUserId, "Flott, jeg melder meg på!", "12:40", true)
            )
            5 -> listOf(
                ChatMessage(17, 5, 105, "Takk for sist! Det var gøy 🥏", "15:20", false),
                ChatMessage(18, 5, currentUserId, "Ja, det var virkelig morsomt!", "15:25", true),
                ChatMessage(19, 5, 105, "Må gjøre det igjen snart", "15:26", false),
                ChatMessage(20, 5, currentUserId, "Absolutt! Neste helg kanskje?", "15:30", true),
                ChatMessage(21, 5, 105, "Perfekt! 👍", "15:31", false)
            )
            else -> emptyList()
        }
    }

    /**
     * TODO: Implement when database is ready
     * Future functions to implement:
     * - suspend fun sendMessage(conversationId: Int, text: String): Result<ChatMessage>
     * - suspend fun markAsRead(conversationId: Int): Result<Unit>
     * - suspend fun getConversations(): Flow<List<Conversation>>
     * - suspend fun getMessages(conversationId: Int): Flow<List<ChatMessage>>
     * - fun encryptMessage(text: String): String
     * - fun decryptMessage(encryptedText: String): String
     */
}

