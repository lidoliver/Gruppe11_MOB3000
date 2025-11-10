package my.application.data.repository

import android.content.Context
import my.application.data.auth.AuthManager
import my.application.data.remote.BackendApi
import my.application.data.remote.dto.TrackDto

class TrackRepository(private val context: Context) {
    private val api = BackendApi.service
    private val authManager = AuthManager(context)

    /**
     * Fetch all tracks/courses from the backend
     */
    suspend fun getAllTracks(): Result<List<TrackDto>> {
        return try {
            val response = api.getAllTracks()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch tracks: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch a specific track by ID
     */
    suspend fun getTrackById(baneId: String): Result<TrackDto> {
        return try {
            val token = authManager.getAuthToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token available"))
            }

            val response = api.getTrackById(baneId, "Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Track not found"))
            } else {
                Result.failure(Exception("Failed to fetch track: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

