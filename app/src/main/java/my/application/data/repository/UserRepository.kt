package my.application.data.repository

import android.content.Context
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
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val response = api.getUser(email, token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get user: ${response.message()}"))
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
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val response = api.getScorecards(email, token)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to get scorecards: ${response.message()}"))
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

