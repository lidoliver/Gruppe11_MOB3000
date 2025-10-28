package my.application.data.network

import my.application.data.network.dto.ClubDto
import my.application.data.network.dto.ClubJoinRequestDto
import my.application.data.network.dto.ClubJoinResponseDto
import my.application.data.network.dto.ClubRequestDto
import my.application.data.network.dto.ClubRequestResponseDto
import my.application.data.network.dto.ConversationDto
import my.application.data.network.dto.CreateClubRequestDto
import my.application.data.network.dto.FriendRequestDto
import my.application.data.network.dto.MessageDto
import my.application.data.network.dto.MessageResponseDto
import my.application.data.network.dto.PendingJoinRequestDto
import my.application.data.network.dto.SendFriendRequestDto
import my.application.data.network.dto.SendMessageDto
import my.application.data.network.dto.UserDto
import my.application.data.network.dto.UserRoleDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Club Join Requests
    @POST("clubJoinRequests")
    suspend fun submitJoinRequest(@Body request: ClubJoinRequestDto): ClubJoinResponseDto

    @GET("clubJoinRequests/club/{klubId}")
    suspend fun getPendingJoinRequests(@Path("klubId") clubId: Int): List<PendingJoinRequestDto>

    @POST("clubJoinRequests/{id}/approve")
    suspend fun approveJoinRequest(@Path("id") requestId: Int): MessageResponseDto

    @POST("clubJoinRequests/{id}/reject")
    suspend fun rejectJoinRequest(@Path("id") requestId: Int): MessageResponseDto

    // User
    @GET("me")
    suspend fun getMe(): UserRoleDto

    // Users
    @GET("users")
    suspend fun getAllUsers(): List<UserDto>

    @GET("users/search")
    suspend fun searchUsersByEmail(@Query("email") email: String): List<UserDto>

    @GET("users/search-users")
    suspend fun searchUsers(@Query("q") searchTerm: String): List<UserDto>

    @DELETE("users/{email}")
    suspend fun deleteUser(@Path("email") email: String): MessageResponseDto

    @PUT("users/{email}/ban")
    suspend fun banUser(@Path("email") email: String): MessageResponseDto

    @PUT("users/{email}/unban")
    suspend fun unbanUser(@Path("email") email: String): MessageResponseDto

    // Friends
    @POST("friends/request")
    suspend fun sendFriendRequest(@Body friendRequest: SendFriendRequestDto): FriendRequestDto

    @GET("friends/requests")
    suspend fun getFriendRequests(): List<FriendRequestDto>

    @POST("friends/accept/{requestId}")
    suspend fun acceptFriendRequest(@Path("requestId") requestId: Int): FriendRequestDto

    @POST("friends/decline/{requestId}")
    suspend fun declineFriendRequest(@Path("requestId") requestId: Int): FriendRequestDto

    @GET("friends")
    suspend fun getFriends(): List<UserDto>

    @DELETE("friends/{friendEmail}")
    suspend fun removeFriend(@Path("friendEmail") friendEmail: String): MessageResponseDto

    // Messages
    @POST("messages")
    suspend fun sendMessage(@Body message: SendMessageDto): MessageDto

    @GET("messages/conversations")
    suspend fun getConversations(): List<ConversationDto>

    @GET("messages/conversation/{contactEmail}")
    suspend fun getMessages(@Path("contactEmail") contactEmail: String): List<MessageDto>

    // Club Requests
    @POST("clubRequests")
    suspend fun submitClubRequest(@Body clubRequest: CreateClubRequestDto): ClubRequestResponseDto

    @GET("clubRequests")
    suspend fun getPendingClubRequests(): List<ClubRequestDto>

    @POST("clubRequests/{id}/approve")
    suspend fun approveClubRequest(@Path("id") requestId: Int): MessageResponseDto

    @POST("clubRequests/{id}/reject")
    suspend fun rejectClubRequest(@Path("id") requestId: Int): MessageResponseDto

    // Club Management
    @GET("clubManagement/my-club")
    suspend fun getMyClub(): ClubDto

    @GET("clubManagement/clubs/{clubId}/members")
    suspend fun getClubMembers(@Path("clubId") clubId: Int): List<UserDto>

    @DELETE("clubManagement/clubs/{clubId}/members/{email}")
    suspend fun removeClubMember(@Path("clubId") clubId: Int, @Path("email") email: String): MessageResponseDto
}
