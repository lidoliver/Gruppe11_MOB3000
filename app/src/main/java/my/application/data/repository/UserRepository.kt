package my.application.data.repository

import android.util.Log
import my.application.data.remote.BackendApi
import my.application.data.remote.dto.LoginRequest
import my.application.data.remote.dto.LoginResponse
import my.application.data.remote.dto.RegisterRequest
import my.application.data.remote.dto.RegisterResponse
import my.application.data.remote.dto.ScorecardDto
import my.application.data.remote.dto.UserDto

class UserRepository {
    private val api = BackendApi.service
    private var authToken: String? = null
    private var userEmail: String? = null

    fun setAuthToken(token: String) {
        authToken = "Bearer $token"
    }

    fun getAuthToken(): String? = authToken

    fun setUserEmail(email: String) {
        userEmail = email
    }

    fun getUserEmail(): String? = userEmail

    suspend fun register(email: String, password: String): Result<RegisterResponse> {
        return try {
            val response = api.register(RegisterRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "User already exists"
                    else -> "Registration failed: ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Registration error", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                setAuthToken(loginResponse.token)
                setUserEmail(loginResponse.email)
                Result.success(loginResponse)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Login error", e)
            Result.failure(e)
        }
    }

    suspend fun getUser(email: String): Result<UserDto> {
        return try {
            val token = authToken ?: return Result.failure(AuthException("Not authenticated"))
            val response = api.getUser(email, token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                // Sjekk for autentiseringsfeil
                if (response.code() == 401 || response.code() == 403) {
                    Result.failure(AuthException("Authentication failed"))
                } else {
                    Result.failure(Exception("Failed to get user: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Get user error", e)
            Result.failure(e)
        }
    }

    suspend fun updateUser(email: String, user: UserDto): Result<UserDto> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val response = api.updateUser(email, user, token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update user: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Update user error", e)
            Result.failure(e)
        }
    }

    suspend fun getScorecards(email: String): Result<List<ScorecardDto>> {
        return try {
            Log.d("UserRepository", "Fetching scorecards for email: $email")
            val token = authToken ?: return Result.failure(AuthException("Not authenticated"))
            val response = api.getScorecards(email, token)
            Log.d("UserRepository", "Scorecards response code: ${response.code()}")
            if (response.isSuccessful) {
                val scorecards = response.body() ?: emptyList()
                Log.d("UserRepository", "Scorecards fetched successfully: ${scorecards.size} items")
                Result.success(scorecards)
            } else {
                Log.e("UserRepository", "Failed to get scorecards: ${response.code()} - ${response.message()}")
                // Sjekk for autentiseringsfeil
                if (response.code() == 401 || response.code() == 403) {
                    Result.failure(AuthException("Authentication failed"))
                } else {
                    Result.failure(Exception("Failed to get scorecards: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Get scorecards error", e)
            Result.failure(e)
        }
    }

    suspend fun getAllPlayers(): Result<List<UserDto>> {
        return try {
            val response = api.getAllPlayers()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to get players: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Get all players error", e)
            Result.failure(e)
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<String> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val request = my.application.data.remote.dto.ChangePasswordRequest(currentPassword, newPassword)
            val response = api.changePassword(request, token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.message)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Current password is incorrect"
                    400 -> "Invalid password format"
                    else -> "Failed to change password: ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Change password error", e)
            Result.failure(e)
        }
    }

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val response = api.checkUsernameAvailability(username, token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.available)
            } else {
                Result.failure(Exception("Failed to check username: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Check username error", e)
            Result.failure(e)
        }
    }

    suspend fun getFriends(): Result<List<my.application.data.remote.dto.FriendDto>> {
        return try {
            val token = authToken ?: return Result.failure(AuthException("Not authenticated"))
            val response = api.getFriends(token)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                if (response.code() == 401 || response.code() == 403) {
                    Result.failure(AuthException("Authentication failed"))
                } else {
                    Result.failure(Exception("Failed to get friends: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Get friends error", e)
            Result.failure(e)
        }
    }

    suspend fun createScorecard(
        baneId: String,
        spillerNavn: String,
        hullPoeng: List<my.application.data.remote.dto.HoleScore>
    ): Result<ScorecardDto> {
        return try {
            val token = authToken ?: return Result.failure(AuthException("Not authenticated"))
            val request = my.application.data.remote.dto.CreateScorecardRequest(
                baneId = baneId,
                spillerNavn = spillerNavn,
                hullPoeng = hullPoeng
            )
            val response = api.createScorecard(request, token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                if (response.code() == 401 || response.code() == 403) {
                    Result.failure(AuthException("Authentication failed"))
                } else {
                    Result.failure(Exception("Failed to create scorecard: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Create scorecard error", e)
            Result.failure(e)
        }
    }

    /**
     * Send heartbeat to backend to update last active status
     * This should be called periodically to keep online status updated
     */
    suspend fun updateHeartbeat(): Result<Unit> {
        return try {
            val token = authToken ?: return Result.failure(AuthException("Not authenticated"))
            val response = api.updateHeartbeat(token)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                // Don't log heartbeat failures as errors since they're routine
                Result.failure(Exception("Heartbeat failed"))
            }
        } catch (e: Exception) {
            // Don't log heartbeat failures as errors
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var instance: UserRepository? = null

        fun getInstance(): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository().also { instance = it }
            }
        }
    }
}

// Custom exception for authentication errors
class AuthException(message: String) : Exception(message)

