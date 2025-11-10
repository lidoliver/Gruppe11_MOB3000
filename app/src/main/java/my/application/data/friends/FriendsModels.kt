package my.application.data.friends

/**
 * Data models for friends feature
 * Domain models used throughout the app for representing friend data
 */

/**
 * Represents a friend in the friends list
 * Domain model populated from backend API via FriendsRepository
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
 * Future enhancement: Will be populated from game history data
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

