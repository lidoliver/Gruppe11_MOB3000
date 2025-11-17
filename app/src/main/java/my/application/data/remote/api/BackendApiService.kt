package my.application.data.remote.api

import my.application.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface BackendApiService {

    // Authentication endpoints
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

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

    // Search users endpoint
    @GET("users/search-users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Header("Authorization") token: String
    ): Response<List<UserDto>>

    // Friends endpoints
    @GET("api/friends")
    suspend fun getFriends(
        @Header("Authorization") token: String
    ): Response<List<FriendDto>>

    @GET("api/friends/requests")
    suspend fun getFriendRequests(
        @Header("Authorization") token: String
    ): Response<List<FriendRequestDto>>

    @POST("api/friends/request")
    suspend fun sendFriendRequest(
        @Body request: SendFriendRequestDto,
        @Header("Authorization") token: String
    ): Response<FriendRequestResponseDto>

    @POST("api/friends/accept/{requestId}")
    suspend fun acceptFriendRequest(
        @Path("requestId") requestId: Int,
        @Header("Authorization") token: String
    ): Response<FriendRequestResponseDto>

    @POST("api/friends/decline/{requestId}")
    suspend fun declineFriendRequest(
        @Path("requestId") requestId: Int,
        @Header("Authorization") token: String
    ): Response<FriendRequestResponseDto>

    @DELETE("api/friends/{friendEmail}")
    suspend fun removeFriend(
        @Path("friendEmail") friendEmail: String,
        @Header("Authorization") token: String
    ): Response<FriendRequestResponseDto>

    // Course/Track endpoints
    @GET("api/baneoversikt")
    suspend fun getAllTracks(): Response<List<TrackDto>>

    @GET("api/bane/{bane_id}")
    suspend fun getTrackById(
        @Path("bane_id") baneId: String,
        @Header("Authorization") token: String
    ): Response<TrackDto>

    // Message endpoints
    @GET("api/messages/conversations")
    suspend fun getConversations(
        @Header("Authorization") token: String
    ): Response<List<ConversationDto>>

    @GET("api/messages/conversation/{contactEmail}")
    suspend fun getMessages(
        @Path("contactEmail") contactEmail: String,
        @Header("Authorization") token: String
    ): Response<List<MessageDto>>

    @POST("api/messages")
    suspend fun sendMessage(
        @Body request: SendMessageRequest,
        @Header("Authorization") token: String
    ): Response<SendMessageResponse>

    // Change password endpoint
    @POST("users/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest,
        @Header("Authorization") token: String
    ): Response<ChangePasswordResponse>
}

