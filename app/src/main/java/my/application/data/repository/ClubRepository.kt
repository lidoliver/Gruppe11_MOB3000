package my.application.data.repository

import android.util.Log
import my.application.data.remote.BackendApi
import my.application.data.remote.dto.ClubDto
import my.application.data.remote.dto.SubmitClubRequestDto
import my.application.data.remote.dto.SubmitClubJoinRequestDto

/**
 * Repository for håndtering av klubb-relaterte operasjoner
 * Inkluderer henting av klubber, innsending av klubbforespørsler, og administrering av medlemskap
 */
class ClubRepository private constructor() {
    private val api = BackendApi.service
    private val userRepository = UserRepository.getInstance()

    /**
     * Henter alle klubber fra backend
     */
    suspend fun getAllClubs(): Result<List<ClubDto>> {
        return try {
            Log.d("ClubRepository", "Fetching all clubs")

            val token = userRepository.getAuthToken()
            if (token == null) {
                Log.e("ClubRepository", "No auth token available")
                return Result.failure(Exception("Not authenticated"))
            }

            val response = api.getAllClubs(token)
            Log.d("ClubRepository", "API response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val clubs = response.body()!!
                Log.d("ClubRepository", "Fetched ${clubs.size} clubs")
                Result.success(clubs)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Failed to fetch clubs: ${response.code()} - ${response.message()}\nError body: $errorBody"
                Log.e("ClubRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ClubRepository", "Fetch clubs error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sender inn en forespørsel om å bli medlem i en klubb
     * @param clubId ID-en til klubben bruker ønsker å bli med i
     * @param message Valgfri melding til klubbadmin
     */
    suspend fun submitClubJoinRequest(clubId: Int, message: String?): Result<String> {
        return try {
            Log.d("ClubRepository", "Submitting club join request for club ID: $clubId")

            val token = userRepository.getAuthToken()
            if (token == null) {
                Log.e("ClubRepository", "No auth token available")
                return Result.failure(Exception("Not authenticated"))
            }

            val request = SubmitClubJoinRequestDto(
                clubId = clubId,
                message = message
            )

            val response = api.submitClubJoinRequest(request, token)
            Log.d("ClubRepository", "API response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val responseMessage = response.body()!!.message
                Log.d("ClubRepository", "Club join request submitted successfully")
                Result.success(responseMessage)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Failed to submit join request: ${response.code()} - ${response.message()}\nError body: $errorBody"
                Log.e("ClubRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ClubRepository", "Submit join request error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Henter medlemsforespørsler for en spesifikk klubb
     * Brukes av klubbadmin for å se og behandle medlemsforespørsler
     * @param clubId ID-en til klubben
     */
    suspend fun getClubJoinRequests(clubId: Int): Result<List<my.application.data.remote.dto.ClubJoinRequestDto>> {
        return try {
            Log.d("ClubRepository", "Fetching join requests for club ID: $clubId")

            val token = userRepository.getAuthToken()
            if (token == null) {
                Log.e("ClubRepository", "No auth token available")
                return Result.failure(Exception("Not authenticated"))
            }

            val response = api.getClubJoinRequests(clubId, token)
            Log.d("ClubRepository", "API response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val requests = response.body()!!
                Log.d("ClubRepository", "Fetched ${requests.size} join requests")
                Result.success(requests)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Failed to fetch join requests: ${response.code()} - ${response.message()}\nError body: $errorBody"
                Log.e("ClubRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ClubRepository", "Fetch join requests error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Godkjenner en medlemsforespørsel
     * Legger til brukeren som medlem av klubben
     * @param requestId ID-en til medlemsforespørselen som skal godkjennes
     */
    suspend fun approveJoinRequest(requestId: Int): Result<String> {
        return try {
            Log.d("ClubRepository", "Approving join request ID: $requestId")

            val token = userRepository.getAuthToken()
            if (token == null) {
                Log.e("ClubRepository", "No auth token available")
                return Result.failure(Exception("Not authenticated"))
            }

            val response = api.approveClubJoinRequest(requestId, token)
            Log.d("ClubRepository", "API response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val message = response.body()!!.message
                Log.d("ClubRepository", "Join request approved successfully")
                Result.success(message)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Failed to approve join request: ${response.code()} - ${response.message()}\nError body: $errorBody"
                Log.e("ClubRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ClubRepository", "Approve join request error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Avviser en medlemsforespørsel
     * Sletter forespørselen fra databasen
     * @param requestId ID-en til medlemsforespørselen som skal avvises
     */
    suspend fun rejectJoinRequest(requestId: Int): Result<String> {
        return try {
            Log.d("ClubRepository", "Rejecting join request ID: $requestId")

            val token = userRepository.getAuthToken()
            if (token == null) {
                Log.e("ClubRepository", "No auth token available")
                return Result.failure(Exception("Not authenticated"))
            }

            val response = api.rejectClubJoinRequest(requestId, token)
            Log.d("ClubRepository", "API response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val message = response.body()!!.message
                Log.d("ClubRepository", "Join request rejected successfully")
                Result.success(message)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Failed to reject join request: ${response.code()} - ${response.message()}\nError body: $errorBody"
                Log.e("ClubRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ClubRepository", "Reject join request error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sender inn en forespørsel om å opprette en ny klubb
     * Forespørselen sendes til admin for godkjenning
     * @param clubName Navnet på klubben som skal opprettes
     * @param description Beskrivelse/søknadsbrev for klubben
     */
    suspend fun submitClubRequest(clubName: String, description: String, location: String? = null): Result<String> {
        return try {
            Log.d("ClubRepository", "Submitting club request: clubName='$clubName', location='$location', description length=${description.length}")

            val token = userRepository.getAuthToken()
            if (token == null) {
                Log.e("ClubRepository", "No auth token available")
                return Result.failure(Exception("Not authenticated"))
            }

            val request = SubmitClubRequestDto(
                clubName = clubName,
                location = location,
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

