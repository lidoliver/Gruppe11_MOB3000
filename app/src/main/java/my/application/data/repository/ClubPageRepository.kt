package my.application.data.repository

import android.util.Log
import my.application.data.auth.AuthManager
import my.application.data.remote.api.BackendApiService
import my.application.data.remote.dto.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Repository for klubbside-operasjoner
 * Håndterer kunngjøringer, arrangementer og deltakelse for klubber
 * Bruker Retrofit og Moshi for nettverkskommunikasjon
 */
class ClubPageRepository private constructor() {
    // Moshi JSON-konverterer for parsing av data
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Retrofit-instans for API-kall
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5000/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(BackendApiService::class.java)
    private var authToken: String? = null

    /** Setter autentiseringstoken for API-kall */
    /** Setter autentiseringstoken for API-kall */
    fun setAuthToken(token: String?) {
        this.authToken = token
    }

    /**
     * Henter klubbsiden med all informasjon
     * @param clubId ID til klubben som skal hentes
     * @return Result med ClubPageDto inkl. kunngjøringer og arrangementer
     */
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

    /**
     * Oppretter en ny kunngjøring for en klubb
     * @param clubId ID til klubben
     * @param content Innholdet i kunngjøringen
     * @return Result med opprettet ClubAnnouncementDto
     */
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

    /**
     * Sletter en kunngjøring
     * @param clubId ID til klubben
     * @param announcementId ID til kunngjøringen som skal slettes
     * @return Result med Unit ved suksess
     */
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

    /**
     * Oppretter et nytt arrangement for en klubb
     * @param clubId ID til klubben
     * @param title Tittelen på arrangementet
     * @param description Beskrivelse av arrangementet (valgfritt)
     * @param eventDate Dato for arrangementet
     * @return Result med opprettet ClubEventDto
     */
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

    /**
     * Sletter et arrangement
     * @param clubId ID til klubben
     * @param eventId ID til arrangementet som skal slettes
     * @return Result med Unit ved suksess
     */
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

    /**
     * Melder på en bruker til et arrangement
     * @param clubId ID til klubben
     * @param eventId ID til arrangementet brukeren skal melde seg på
     * @return Result med Unit ved suksess
     */
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

    /**
     * Melder av en bruker fra et arrangement
     * @param clubId ID til klubben
     * @param eventId ID til arrangementet brukeren skal melde seg av
     * @return Result med Unit ved suksess
     */
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

    /**
     * Henter alle deltakere i et arrangement
     * @param clubId ID til klubben
     * @param eventId ID til arrangementet
     * @return Result med liste over EventParticipantDto
     */
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

        /** Henter singleton-instansen av ClubPageRepository */
        fun getInstance(): ClubPageRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ClubPageRepository().also { INSTANCE = it }
            }
        }
    }
}

