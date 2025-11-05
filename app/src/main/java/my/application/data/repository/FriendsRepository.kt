package my.application.data.repository

import android.util.Log
import my.application.data.friends.Friend
import my.application.data.friends.FriendStatus
import my.application.data.remote.BackendApi
import my.application.data.remote.dto.FriendDto
import my.application.data.remote.dto.FriendRequestDto
import my.application.data.remote.dto.SendFriendRequestDto
import my.application.data.remote.dto.UserDto
import kotlin.random.Random

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
        return Friend(
            id = dto.id,
            userId = 0, // Not provided by backend, could be added later
            name = dto.friendUsername,
            username = dto.friendUsername,
            status = FriendStatus.OFFLINE, // Default status, can be enhanced with real-time status
            lastPlayed = null, // Can be enhanced with actual data
            gamesPlayed = 0, // Can be enhanced with actual data
            avatarColor = generateColorForUsername(dto.friendUsername),
            hasUnreadMessages = false // Can be enhanced with message system
        )
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

