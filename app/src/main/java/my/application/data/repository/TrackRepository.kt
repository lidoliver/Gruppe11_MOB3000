package my.application.data.repository

import android.content.Context
import my.application.data.auth.AuthManager
import my.application.data.remote.BackendApi
import my.application.data.remote.dto.TrackDto

/**
 * Repository for bane/track-relaterte operasjoner
 * Håndterer henting av disc golf-baner fra backend
 * Inneholder også GPS-data for hull på hver bane
 */
class TrackRepository(private val context: Context) {
    private val api = BackendApi.service
    private val authManager = AuthManager(context)

    /**
     * Henter alle baner/tracks fra backend
     * Offentlig tilgjengelig uten autentisering
     * @return Result med liste over TrackDto ved suksess
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
     * Henter en spesifikk bane basert på ID
     * Krever autentisering for å få tilgang til detaljert informasjon inkl. GPS-data
     * @param baneId ID til banen som skal hentes
     * @return Result med TrackDto inkl. hulldata ved suksess
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

