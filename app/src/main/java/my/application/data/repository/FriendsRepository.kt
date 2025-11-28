package my.application.data.repository

import android.util.Log
import my.application.data.friends.Friend
import my.application.data.friends.FriendStatus
import my.application.data.remote.BackendApi
import my.application.data.remote.dto.FriendDto
import my.application.data.remote.dto.FriendRequestDto
import my.application.data.remote.dto.SendFriendRequestDto
import my.application.data.remote.dto.UserDto
/**
 * Repository for managing friends data
 * Handles communication with backend API for friends functionality
 */
class FriendsRepository private constructor() {
    private val api = BackendApi.service
    private val userRepository = UserRepository.getInstance()

    /**
     * Fetch friends list from backend
     */
    suspend fun getFriends(): Result<List<Friend>> {
        return try {
            Log.d("FriendsRepository", "Starting getFriends()")

            val token = userRepository.getAuthToken()
            if (token == null) {
                Log.e("FriendsRepository", "No auth token available")
                return Result.failure(Exception("Not authenticated"))
            }
            Log.d("FriendsRepository", "Auth token exists: ${token.take(20)}...")

            Log.d("FriendsRepository", "Calling API getFriends")
            val response = api.getFriends(token)
            Log.d("FriendsRepository", "API response code: ${response.code()}")
            Log.d("FriendsRepository", "API response successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val friendDtos = response.body()!!
                Log.d("FriendsRepository", "Received ${friendDtos.size} friends from API")
                val friends = friendDtos.map { dto -> mapDtoToFriend(dto) }
                Log.d("FriendsRepository", "Mapped to ${friends.size} Friend objects")
                Result.success(friends)
            } else {
                val errorMsg = "Failed to get friends: ${response.code()} - ${response.message()}"
                Log.e("FriendsRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("FriendsRepository", "Get friends error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Search for users by name, username, or email
     */
    suspend fun searchUsers(query: String): Result<List<UserDto>> {
        return try {
            Log.d("FriendsRepository", "Starting searchUsers with query: '$query'")

            val token = userRepository.getAuthToken()
            if (token == null) {
                Log.e("FriendsRepository", "No auth token available for search")
                return Result.failure(Exception("Not authenticated"))
            }
            Log.d("FriendsRepository", "Auth token exists for search")

            Log.d("FriendsRepository", "Calling API searchUsers")
            val response = api.searchUsers(query, token)
            Log.d("FriendsRepository", "Search API response code: ${response.code()}")
            Log.d("FriendsRepository", "Search API response successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val users = response.body()!!
                Log.d("FriendsRepository", "Received ${users.size} users from search API")
                Result.success(users)
            } else {
                val errorMsg = "Failed to search users: ${response.code()} - ${response.message()}"
                Log.e("FriendsRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("FriendsRepository", "Search users error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch pending friend requests from backend
     */
    suspend fun getFriendRequests(): Result<List<FriendRequestDto>> {
        return try {
            val token = userRepository.getAuthToken()
                ?: return Result.failure(Exception("Not authenticated"))

            val response = api.getFriendRequests(token)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get friend requests: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("FriendsRepository", "Get friend requests error", e)
            Result.failure(e)
        }
    }

    /**
     * Send a friend request to another user
     */
    suspend fun sendFriendRequest(friendEmail: String): Result<String> {
        return try {
            val token = userRepository.getAuthToken()
                ?: return Result.failure(Exception("Not authenticated"))

            val response = api.sendFriendRequest(SendFriendRequestDto(friendEmail), token)

            if (response.isSuccessful) {
                Result.success("Friend request sent")
            } else {
                Result.failure(Exception("Failed to send friend request: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("FriendsRepository", "Send friend request error", e)
            Result.failure(e)
        }
    }

    /**
     * Accept a friend request
     */
    suspend fun acceptFriendRequest(requestId: Int): Result<String> {
        return try {
            val token = userRepository.getAuthToken()
                ?: return Result.failure(Exception("Not authenticated"))

            val response = api.acceptFriendRequest(requestId, token)

            if (response.isSuccessful) {
                Result.success("Friend request accepted")
            } else {
                Result.failure(Exception("Failed to accept friend request: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("FriendsRepository", "Accept friend request error", e)
            Result.failure(e)
        }
    }

    /**
     * Decline a friend request
     */
    suspend fun declineFriendRequest(requestId: Int): Result<String> {
        return try {
            val token = userRepository.getAuthToken()
                ?: return Result.failure(Exception("Not authenticated"))

            val response = api.declineFriendRequest(requestId, token)

            if (response.isSuccessful) {
                Result.success("Friend request declined")
            } else {
                Result.failure(Exception("Failed to decline friend request: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("FriendsRepository", "Decline friend request error", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a friend
     */
    suspend fun removeFriend(friendEmail: String): Result<String> {
        return try {
            val token = userRepository.getAuthToken()
                ?: return Result.failure(Exception("Not authenticated"))

            val response = api.removeFriend(friendEmail, token)

            if (response.isSuccessful) {
                Result.success("Friend removed")
            } else {
                Result.failure(Exception("Failed to remove friend: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("FriendsRepository", "Remove friend error", e)
            Result.failure(e)
        }
    }

    /**
     * Map DTO to Friend domain model
     */
    private fun mapDtoToFriend(dto: FriendDto): Friend {
        // Build full name from first and last name, fall back to username if not available
        val fullName = when {
            !dto.friendFirstName.isNullOrBlank() && !dto.friendLastName.isNullOrBlank() ->
                "${dto.friendFirstName} ${dto.friendLastName}"
            !dto.friendFirstName.isNullOrBlank() -> dto.friendFirstName
            else -> dto.friendUsername
        }

        // Format last played date
        val lastPlayedText = dto.lastPlayed?.let { formatLastPlayed(it) }

        // Map online status from backend
        val onlineStatus = when (dto.onlineStatus?.lowercase()) {
            "online" -> FriendStatus.ONLINE
            "in_game" -> FriendStatus.IN_GAME
            else -> FriendStatus.OFFLINE
        }

        return Friend(
            id = dto.id,
            userId = 0, // Not needed for current use case
            name = fullName,
            username = dto.friendUsername,
            email = dto.friendEmail,
            status = onlineStatus,
            lastPlayed = lastPlayedText,
            gamesPlayed = dto.gamesPlayed,
            avatarColor = generateColorForUsername(dto.friendUsername),
            hasUnreadMessages = false // Notification badges disabled
        )
    }

    /**
     * Format last played timestamp to user-friendly text
     */
    private fun formatLastPlayed(timestamp: String): String {
        return try {
            // Backend now sends timestamps in format "2025-11-28T13:55:49" (already in local time with +2h offset)
            val lastPlayedTime = java.time.LocalDateTime.parse(timestamp.substringBefore("+").substringBefore("Z"))
            val now = java.time.LocalDateTime.now()

            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastPlayedTime.toLocalDate(), now.toLocalDate())
            val hoursBetween = java.time.temporal.ChronoUnit.HOURS.between(lastPlayedTime, now)
            val minutesBetween = java.time.temporal.ChronoUnit.MINUTES.between(lastPlayedTime, now)

            when {
                minutesBetween < 1 -> "Akkurat nå"
                minutesBetween < 60 -> "$minutesBetween min siden"
                hoursBetween < 24 -> "$hoursBetween timer siden"
                daysBetween == 1L -> "I går"
                daysBetween < 7 -> "$daysBetween dager siden"
                daysBetween < 30 -> "${daysBetween / 7} uker siden"
                else -> {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    lastPlayedTime.format(formatter)
                }
            }
        } catch (e: Exception) {
            Log.w("FriendsRepository", "Failed to parse timestamp: $timestamp", e)
            timestamp.substringBefore('T')
        }
    }

    /**
     * Generate a consistent color for a username
     */
    private fun generateColorForUsername(username: String): Long {
        val colors = listOf(
            0xFF1976D2, // Blue
            0xFF388E3C, // Green
            0xFFD32F2F, // Red
            0xFF7B1FA2, // Purple
            0xFFF57C00, // Orange
            0xFF0097A7, // Cyan
            0xFFC2185B, // Pink
            0xFF5D4037, // Brown
            0xFF455A64  // Blue Grey
        )

        val hash = username.hashCode()
        val index = Math.abs(hash) % colors.size
        return colors[index]
    }

    companion object {
        @Volatile
        private var instance: FriendsRepository? = null

        fun getInstance(): FriendsRepository {
            return instance ?: synchronized(this) {
                instance ?: FriendsRepository().also { instance = it }
            }
        }
    }
}

