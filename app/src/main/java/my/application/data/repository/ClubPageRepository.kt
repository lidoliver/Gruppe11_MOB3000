package my.application.data.repository

import android.util.Log
import my.application.data.auth.AuthManager
import my.application.data.remote.api.BackendApiService
import my.application.data.remote.dto.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class ClubPageRepository private constructor() {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://gruppe11-discgolf-app-e20a44954943.herokuapp.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(BackendApiService::class.java)
    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        this.authToken = token
    }

    suspend fun getClubPage(clubId: Int): Result<ClubPageDto> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val response = api.getClubPage(clubId, token)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ClubPageRepository", "Get club page failed: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to get club page: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("ClubPageRepository", "Get club page error", e)
            Result.failure(e)
        }
    }

    suspend fun createAnnouncement(clubId: Int, content: String): Result<ClubAnnouncementDto> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val request = CreateAnnouncementRequest(content)
            val response = api.createAnnouncement(clubId, request, token)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ClubPageRepository", "Create announcement failed: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to create announcement"))
            }
        } catch (e: Exception) {
            Log.e("ClubPageRepository", "Create announcement error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(clubId: Int, announcementId: Int): Result<Unit> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val response = api.deleteAnnouncement(clubId, announcementId, token)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete announcement"))
            }
        } catch (e: Exception) {
            Log.e("ClubPageRepository", "Delete announcement error", e)
            Result.failure(e)
        }
    }

    suspend fun createEvent(clubId: Int, title: String, description: String?, eventDate: String): Result<ClubEventDto> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val request = CreateEventRequest(title, description, eventDate)
            val response = api.createEvent(clubId, request, token)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ClubPageRepository", "Create event failed: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to create event"))
            }
        } catch (e: Exception) {
            Log.e("ClubPageRepository", "Create event error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(clubId: Int, eventId: Int): Result<Unit> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val response = api.deleteEvent(clubId, eventId, token)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete event"))
            }
        } catch (e: Exception) {
            Log.e("ClubPageRepository", "Delete event error", e)
            Result.failure(e)
        }
    }

    suspend fun joinEvent(clubId: Int, eventId: Int): Result<Unit> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val response = api.joinEvent(clubId, eventId, token)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ClubPageRepository", "Join event failed: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to join event"))
            }
        } catch (e: Exception) {
            Log.e("ClubPageRepository", "Join event error", e)
            Result.failure(e)
        }
    }

    suspend fun leaveEvent(clubId: Int, eventId: Int): Result<Unit> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val response = api.leaveEvent(clubId, eventId, token)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to leave event"))
            }
        } catch (e: Exception) {
            Log.e("ClubPageRepository", "Leave event error", e)
            Result.failure(e)
        }
    }

    suspend fun getEventParticipants(clubId: Int, eventId: Int): Result<List<EventParticipantDto>> {
        return try {
            val token = authToken ?: return Result.failure(Exception("Not authenticated"))
            val response = api.getEventParticipants(clubId, eventId, token)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get participants"))
            }
        } catch (e: Exception) {
            Log.e("ClubPageRepository", "Get participants error", e)
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ClubPageRepository? = null

        fun getInstance(): ClubPageRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ClubPageRepository().also { INSTANCE = it }
            }
        }
    }
}

