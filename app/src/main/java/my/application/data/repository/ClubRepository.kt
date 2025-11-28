package my.application.data.repository

import android.util.Log
import my.application.data.remote.BackendApi
import my.application.data.remote.dto.SubmitClubRequestDto

/**
 * Repository for managing club-related operations
 */
class ClubRepository private constructor() {
    private val api = BackendApi.service
    private val userRepository = UserRepository.getInstance()

    /**
     * Submit a club creation request
     */
    suspend fun submitClubRequest(clubName: String, description: String): Result<String> {
        return try {
            Log.d("ClubRepository", "Submitting club request: clubName='$clubName', description length=${description.length}")

            val token = userRepository.getAuthToken()
            if (token == null) {
                Log.e("ClubRepository", "No auth token available")
                return Result.failure(Exception("Not authenticated"))
            }

            val request = SubmitClubRequestDto(
                clubName = clubName,
                description = description
            )

            Log.d("ClubRepository", "Request DTO: clubName=${request.clubName}, description=${request.description.take(50)}")
            Log.d("ClubRepository", "Auth token: ${token.take(20)}...")

            val response = api.submitClubRequest(request, token)
            Log.d("ClubRepository", "API response code: ${response.code()}")
            Log.d("ClubRepository", "API response message: ${response.message()}")

            if (response.isSuccessful && response.body() != null) {
                val message = response.body()!!.message
                Log.d("ClubRepository", "Club request submitted successfully: $message")
                Result.success(message)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Failed to submit club request: ${response.code()} - ${response.message()}\nError body: $errorBody"
                Log.e("ClubRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ClubRepository", "Submit club request error: ${e.message}", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var instance: ClubRepository? = null

        fun getInstance(): ClubRepository {
            return instance ?: synchronized(this) {
                instance ?: ClubRepository().also { instance = it }
            }
        }
    }
}

