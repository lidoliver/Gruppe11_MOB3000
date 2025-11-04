package my.application.data.remote.api

import my.application.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface BackendApiService {

    // Authentication endpoints
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // User endpoints
    @GET("bruker/{email}")
    suspend fun getUser(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<UserDto>

    @PUT("bruker/{email}")
    suspend fun updateUser(
        @Path("email") email: String,
        @Body user: UserDto,
        @Header("Authorization") token: String
    ): Response<UserDto>

    @DELETE("bruker/{email}")
    suspend fun deleteUser(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<Unit>

    // Scorecard endpoints
    @GET("api/pointcard/{email}")
    suspend fun getScorecards(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<List<ScorecardDto>>

    // Players endpoint
    @GET("api/epost")
    suspend fun getAllPlayers(): Response<List<UserDto>>
}

