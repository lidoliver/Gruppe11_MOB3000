package my.application.data.friends

/**
 * Data models for friends feature
 * These models mirror the database structure and will be used for both demo and real data
 */

/**
 * Represents a friend in the friends list
 * This will be populated from the database in the future
 */
data class Friend(
    val id: Int,
    val userId: Int,
    val name: String,
    val username: String,
    val status: FriendStatus = FriendStatus.ONLINE,
    val lastPlayed: String? = null, // Last time they played
    val gamesPlayed: Int = 0,
    val avatarColor: Long, // Stored as Long for database compatibility (will use Color(avatarColor) in UI)
    val hasUnreadMessages: Boolean = false
)

/**
 * Friend online status
 */
enum class FriendStatus {
    ONLINE,
    OFFLINE,
    IN_GAME
}

/**
 * Play history entry for a friend
 * This will be populated from the database in the future
 */
data class PlayHistoryEntry(
    val id: Int,
    val friendId: Int,
    val courseName: String,
    val score: Int,
    val par: Int,
    val date: String,
    val duration: String? = null
)

/**
 * Repository for demo data
 * TODO: Replace with actual database repository when backend is ready
 */
object FriendsDemoRepository {

    /**
     * Demo friends list - Replace with database query
     * Future: SELECT * FROM friends WHERE user_id = current_user ORDER BY status, name
     */
    fun getDemoFriends(): List<Friend> {
        return listOf(
            Friend(
                id = 1,
                userId = 101,
                name = "Erik Hansen",
                username = "@erikH",
                status = FriendStatus.ONLINE,
                lastPlayed = "2 timer siden",
                gamesPlayed = 45,
                avatarColor = 0xFF6750A4,
                hasUnreadMessages = true
            ),
            Friend(
                id = 2,
                userId = 102,
                name = "Maria Johansen",
                username = "@mariaJ",
                status = FriendStatus.IN_GAME,
                lastPlayed = "Spiller nå",
                gamesPlayed = 32,
                avatarColor = 0xFF625B71,
                hasUnreadMessages = false
            ),
            Friend(
                id = 3,
                userId = 103,
                name = "Lars Olsen",
                username = "@larsO",
                status = FriendStatus.ONLINE,
                lastPlayed = "I går",
                gamesPlayed = 78,
                avatarColor = 0xFF7D5260,
                hasUnreadMessages = false
            ),
            Friend(
                id = 4,
                userId = 104,
                name = "Sofie Berg",
                username = "@sofieB",
                status = FriendStatus.OFFLINE,
                lastPlayed = "3 dager siden",
                gamesPlayed = 23,
                avatarColor = 0xFF8E4585,
                hasUnreadMessages = false
            ),
            Friend(
                id = 5,
                userId = 105,
                name = "Thomas Nilsen",
                username = "@thomasN",
                status = FriendStatus.ONLINE,
                lastPlayed = "1 time siden",
                gamesPlayed = 56,
                avatarColor = 0xFF006874,
                hasUnreadMessages = false
            ),
            Friend(
                id = 6,
                userId = 106,
                name = "Emma Andersen",
                username = "@emmaA",
                status = FriendStatus.OFFLINE,
                lastPlayed = "1 uke siden",
                gamesPlayed = 12,
                avatarColor = 0xFF984061,
                hasUnreadMessages = false
            )
        )
    }

    /**
     * Demo play history for a specific friend - Replace with database query
     * Future: SELECT * FROM game_history WHERE user_id = ? ORDER BY date DESC LIMIT 10
     */
    fun getDemoPlayHistory(friendId: Int): List<PlayHistoryEntry> {
        // This will be implemented when the play history screen is created
        return listOf(
            PlayHistoryEntry(
                id = 1,
                friendId = friendId,
                courseName = "Ekeberg Disc Golf Park",
                score = 58,
                par = 54,
                date = "I dag",
                duration = "1t 45min"
            ),
            PlayHistoryEntry(
                id = 2,
                friendId = friendId,
                courseName = "Sognsvann Disc Golf",
                score = 45,
                par = 48,
                date = "I går",
                duration = "1t 30min"
            ),
            PlayHistoryEntry(
                id = 3,
                friendId = friendId,
                courseName = "Tryvann Disc Golf",
                score = 62,
                par = 60,
                date = "3 dager siden",
                duration = "2t 15min"
            )
        )
    }
}

