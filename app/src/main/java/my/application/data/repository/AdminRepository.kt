package my.application.data.repository

import android.content.Context
import my.application.data.auth.AuthManager
import my.application.data.remote.RetrofitProvider
import my.application.data.remote.dto.AdminResponseDto
import my.application.data.remote.dto.ClubRequestDto
import my.application.data.remote.dto.UserDto

/**
 * Repository for admin-funksjoner
 * Håndterer brukerstyring og klubbforespørsler for administratorer
 */
class AdminRepository private constructor(private val context: Context) {
    private val api = RetrofitProvider.backendApiService
    private val authManager = AuthManager.getInstance(context)

    companion object {
        @Volatile
        private var instance: AdminRepository? = null

        fun getInstance(context: Context): AdminRepository {
            return instance ?: synchronized(this) {
                instance ?: AdminRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    // Brukerstyring-metoder
    /**
     * Henter alle brukere fra backend
     * Krever admin-autentisering
     */
    suspend fun getAllUsers(): Result<List<UserDto>> {
        return try {
            val token = authManager.getAuthToken() ?: return Result.failure(AuthException("No token available"))
            val response = api.getAllUsers("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                // Konverter AdminUserDto-liste til UserDto-liste
                val userDtos = response.body()!!.map { it.toUserDto() }
                Result.success(userDtos)
            } else if (response.code() == 401) {
                Result.failure(AuthException("Unauthorized"))
            } else {
                Result.failure(Exception("Failed to fetch users: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Søker etter brukere basert på søkeord
     * @param query Søkeord (e-post eller navn)
     */
    suspend fun searchUsers(query: String): Result<List<UserDto>> {
        return try {
            val token = authManager.getAuthToken() ?: return Result.failure(AuthException("No token available"))
            val response = api.searchUsers(query, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else if (response.code() == 401) {
                Result.failure(AuthException("Unauthorized"))
            } else {
                Result.failure(Exception("Failed to search users: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Utestenger en bruker
     * @param email E-postadressen til brukeren som skal utestenges
     */
    suspend fun banUser(email: String): Result<AdminResponseDto> {
        return try {
            val token = authManager.getAuthToken() ?: return Result.failure(AuthException("No token available"))
            val response = api.banUser(email, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else if (response.code() == 401) {
                Result.failure(AuthException("Unauthorized"))
            } else {
                Result.failure(Exception("Failed to ban user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fjerner utestengelse av en bruker
     * @param email E-postadressen til brukeren som skal få tilbake tilgang
     */
    suspend fun unbanUser(email: String): Result<AdminResponseDto> {
        return try {
            val token = authManager.getAuthToken() ?: return Result.failure(AuthException("No token available"))
            val response = api.unbanUser(email, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else if (response.code() == 401) {
                Result.failure(AuthException("Unauthorized"))
            } else {
                Result.failure(Exception("Failed to unban user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sletter en bruker permanent
     * @param email E-postadressen til brukeren som skal slettes
     */
    suspend fun deleteUser(email: String): Result<AdminResponseDto> {
        return try {
            val token = authManager.getAuthToken() ?: return Result.failure(AuthException("No token available"))
            val response = api.deleteUser(email, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else if (response.code() == 401) {
                Result.failure(AuthException("Unauthorized"))
            } else {
                Result.failure(Exception("Failed to delete user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Klubbforespørsels-styringsmetoder
    /**
     * Henter alle ventende klubbforespørsler
     * Brukes av admin for å se og behandle forespørsler om opprettelse av klubber
     */
    suspend fun getClubRequests(): Result<List<ClubRequestDto>> {
        return try {
            val token = authManager.getAuthToken() ?: return Result.failure(AuthException("No token available"))
            val response = api.getClubRequests("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else if (response.code() == 401) {
                Result.failure(AuthException("Unauthorized"))
            } else {
                Result.failure(Exception("Failed to fetch club requests: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Godkjenner en klubbforespørsel
     * Oppretter klubben og tildeler personen som sendte forespørselen som klubbadmin
     * @param id ID-en til klubbforespørselen som skal godkjennes
     */
    suspend fun approveClubRequest(id: Int): Result<AdminResponseDto> {
        return try {
            val token = authManager.getAuthToken() ?: return Result.failure(AuthException("No token available"))
            val response = api.approveClubRequest(id, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else if (response.code() == 401) {
                Result.failure(AuthException("Unauthorized"))
            } else {
                Result.failure(Exception("Failed to approve club request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Avviser en klubbforespørsel
     * Sletter forespørselen fra databasen
     * @param id ID-en til klubbforespørselen som skal avvises
     */
    suspend fun rejectClubRequest(id: Int): Result<AdminResponseDto> {
        return try {
            val token = authManager.getAuthToken() ?: return Result.failure(AuthException("No token available"))
            val response = api.rejectClubRequest(id, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else if (response.code() == 401) {
                Result.failure(AuthException("Unauthorized"))
            } else {
                Result.failure(Exception("Failed to reject club request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

