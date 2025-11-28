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


    // Scorecard endpoints
    @GET("api/pointcard/{email}")
    suspend fun getScorecards(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<List<ScorecardDto>>

    @POST("api/pointcard/course")
    suspend fun createScorecard(
        @Body request: CreateScorecardRequest,
        @Header("Authorization") token: String
    ): Response<ScorecardDto>

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

    // Heartbeat endpoint to update last active status
    @POST("api/me/heartbeat")
    suspend fun updateHeartbeat(
        @Header("Authorization") token: String
    ): Response<Unit>

    // Course/Track endpoints
    @GET("api/baneoversikt")
    suspend fun getAllTracks(): Response<List<TrackDto>>

    @GET("api/bane/{bane_id}")
    suspend fun getTrackById(
        @Path("bane_id") baneId: String,
        @Header("Authorization") token: String
    ): Response<TrackDto>

    // Hole management endpoints
    @GET("api/tracks/{trackId}/holes")
    suspend fun getHolesForTrack(
        @Path("trackId") trackId: String,
        @Header("Authorization") token: String
    ): Response<List<HoleData>>

    @PUT("api/tracks/{trackId}/holes/{holeNumber}")
    suspend fun updateHole(
        @Path("trackId") trackId: String,
        @Path("holeNumber") holeNumber: Int,
        @Body hole: HoleData,
        @Header("Authorization") token: String
    ): Response<HoleUpdateResponse>

    @POST("api/tracks/{trackId}/holes/bulk")
    suspend fun bulkUpdateHoles(
        @Path("trackId") trackId: String,
        @Body request: BulkHolesRequest,
        @Header("Authorization") token: String
    ): Response<BulkHolesResponse>

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

    // Check username availability endpoint
    @GET("users/check-username/{username}")
    suspend fun checkUsernameAvailability(
        @Path("username") username: String,
        @Header("Authorization") token: String
    ): Response<UsernameAvailabilityResponse>

    // Admin endpoints - User management
    @GET("users")
    suspend fun getAllUsers(
        @Header("Authorization") token: String
    ): Response<List<AdminUserDto>>

    @PUT("users/{email}/ban")
    suspend fun banUser(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    @PUT("users/{email}/unban")
    suspend fun unbanUser(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    @DELETE("users/{email}")
    suspend fun deleteUser(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    // Club request endpoints
    @POST("api/club-requests")
    suspend fun submitClubRequest(
        @Body request: SubmitClubRequestDto,
        @Header("Authorization") token: String
    ): Response<ClubRequestResponseDto>

    // Admin endpoints - Club requests
    @GET("api/club-requests")
    suspend fun getClubRequests(
        @Header("Authorization") token: String
    ): Response<List<ClubRequestDto>>

    @POST("api/club-requests/{id}/approve")
    suspend fun approveClubRequest(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    @POST("api/club-requests/{id}/reject")
    suspend fun rejectClubRequest(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    // Club endpoints
    @GET("api/clubs")
    suspend fun getAllClubs(
        @Header("Authorization") token: String
    ): Response<List<ClubDto>>

    @GET("api/clubs/{clubId}")
    suspend fun getClubById(
        @Path("clubId") clubId: Int,
        @Header("Authorization") token: String
    ): Response<ClubDto>

    // Club join request endpoints
    @POST("api/club-join-requests")
    suspend fun submitClubJoinRequest(
        @Body request: SubmitClubJoinRequestDto,
        @Header("Authorization") token: String
    ): Response<ClubJoinRequestResponseDto>

    @GET("api/club-join-requests/club/{clubId}")
    suspend fun getClubJoinRequests(
        @Path("clubId") clubId: Int,
        @Header("Authorization") token: String
    ): Response<List<ClubJoinRequestDto>>

    @POST("api/club-join-requests/{id}/approve")
    suspend fun approveClubJoinRequest(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    @POST("api/club-join-requests/{id}/reject")
    suspend fun rejectClubJoinRequest(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    // FCM Token Registration
    @POST("api/notifications/register-token")
    suspend fun registerFcmToken(
        @Body request: RegisterFcmTokenRequest,
        @Header("Authorization") token: String
    ): Response<FcmResponse>
}

