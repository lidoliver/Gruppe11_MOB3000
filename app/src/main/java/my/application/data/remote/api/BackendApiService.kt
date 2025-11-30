package my.application.data.remote.api

import my.application.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API-interface for backend-kommunikasjon
 * Definerer alle HTTP-endepunkter for applikasjonen
 */
interface BackendApiService {

    // Autentiserings-endepunkter
    /** Registrerer ny bruker */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    /** Logger inn eksisterende bruker */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Bruker-endepunkter
    /** Henter brukerdata basert på e-post */
    @GET("bruker/{email}")
    suspend fun getUser(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<UserDto>

    /** Oppdaterer brukerdata */
    @PUT("bruker/{email}")
    suspend fun updateUser(
        @Path("email") email: String,
        @Body user: UserDto,
        @Header("Authorization") token: String
    ): Response<UserDto>


    // Spillkort-endepunkter
    /** Henter alle spillkort for en bruker */
    @GET("api/pointcard/{email}")
    suspend fun getScorecards(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<List<ScorecardDto>>

    /** Oppretter nytt spillkort etter fullført runde */
    @POST("api/pointcard/course")
    suspend fun createScorecard(
        @Body request: CreateScorecardRequest,
        @Header("Authorization") token: String
    ): Response<ScorecardDto>

    // Spiller-endepunkt
    /** Henter alle spillere (e-postadresser) */
    @GET("api/epost")
    suspend fun getAllPlayers(): Response<List<UserDto>>

    // Brukersøk-endepunkt
    /** Søker etter brukere basert på søkeord */
    @GET("users/search-users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Header("Authorization") token: String
    ): Response<List<UserDto>>

    // Venne-endepunkter
    /** Henter alle venner for innlogget bruker */
    @GET("api/friends")
    suspend fun getFriends(
        @Header("Authorization") token: String
    ): Response<List<FriendDto>>

    /** Henter ventende venneforespørsler */
    @GET("api/friends/requests")
    suspend fun getFriendRequests(
        @Header("Authorization") token: String
    ): Response<List<FriendRequestDto>>

    /** Sender venneforespørsel til en annen bruker */
    @POST("api/friends/request")
    suspend fun sendFriendRequest(
        @Body request: SendFriendRequestDto,
        @Header("Authorization") token: String
    ): Response<FriendRequestResponseDto>

    /** Godtar en venneforespørsel */
    @POST("api/friends/accept/{requestId}")
    suspend fun acceptFriendRequest(
        @Path("requestId") requestId: Int,
        @Header("Authorization") token: String
    ): Response<FriendRequestResponseDto>

    /** Avslår en venneforespørsel */
    @POST("api/friends/decline/{requestId}")
    suspend fun declineFriendRequest(
        @Path("requestId") requestId: Int,
        @Header("Authorization") token: String
    ): Response<FriendRequestResponseDto>

    /** Fjerner en venn fra vennelisten */
    @DELETE("api/friends/{friendEmail}")
    suspend fun removeFriend(
        @Path("friendEmail") friendEmail: String,
        @Header("Authorization") token: String
    ): Response<FriendRequestResponseDto>

    // Heartbeat-endepunkt for å oppdatere sist aktiv-status
    /** Sender heartbeat for å vise at bruker er aktiv */
    @POST("api/me/heartbeat")
    suspend fun updateHeartbeat(
        @Header("Authorization") token: String
    ): Response<Unit>

    // Bane/Track-endepunkter
    /** Henter oversikt over alle disc golf-baner */
    @GET("api/baneoversikt")
    suspend fun getAllTracks(): Response<List<TrackDto>>

    /** Henter detaljert informasjon om en spesifikk bane */
    @GET("api/bane/{bane_id}")
    suspend fun getTrackById(
        @Path("bane_id") baneId: String,
        @Header("Authorization") token: String
    ): Response<TrackDto>

    // Hull-administrasjons-endepunkter
    /** Henter alle hull for en bane */
    @GET("api/tracks/{trackId}/holes")
    suspend fun getHolesForTrack(
        @Path("trackId") trackId: String,
        @Header("Authorization") token: String
    ): Response<List<HoleData>>

    /** Oppdaterer et enkelt hull på en bane */
    @PUT("api/tracks/{trackId}/holes/{holeNumber}")
    suspend fun updateHole(
        @Path("trackId") trackId: String,
        @Path("holeNumber") holeNumber: Int,
        @Body hole: HoleData,
        @Header("Authorization") token: String
    ): Response<HoleUpdateResponse>

    /** Oppdaterer flere hull samtidig (bulk-oppdatering) */
    @POST("api/tracks/{trackId}/holes/bulk")
    suspend fun bulkUpdateHoles(
        @Path("trackId") trackId: String,
        @Body request: BulkHolesRequest,
        @Header("Authorization") token: String
    ): Response<BulkHolesResponse>

    // Meldings-endepunkter
    /** Henter alle samtaler for innlogget bruker */
    @GET("api/messages/conversations")
    suspend fun getConversations(
        @Header("Authorization") token: String
    ): Response<List<ConversationDto>>

    /** Henter alle meldinger i en spesifikk samtale */
    @GET("api/messages/conversation/{contactEmail}")
    suspend fun getMessages(
        @Path("contactEmail") contactEmail: String,
        @Header("Authorization") token: String
    ): Response<List<MessageDto>>

    /** Sender en melding til en annen bruker */
    @POST("api/messages")
    suspend fun sendMessage(
        @Body request: SendMessageRequest,
        @Header("Authorization") token: String
    ): Response<SendMessageResponse>

    // Passordendring-endepunkt
    /** Endrer brukerens passord */
    @POST("users/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest,
        @Header("Authorization") token: String
    ): Response<ChangePasswordResponse>

    /** Sjekker om et brukernavn er tilgjengelig */
    @GET("users/check-username/{username}")
    suspend fun checkUsernameAvailability(
        @Path("username") username: String,
        @Header("Authorization") token: String
    ): Response<UsernameAvailabilityResponse>

    // Admin-endepunkter - Brukeradministrasjon
    /** Henter alle brukere (kun for admin) */
    @GET("users")
    suspend fun getAllUsers(
        @Header("Authorization") token: String
    ): Response<List<AdminUserDto>>

    /** Utestenger en bruker (kun for admin) */
    @PUT("users/{email}/ban")
    suspend fun banUser(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    /** Fjerner utestengelse av en bruker (kun for admin) */
    @PUT("users/{email}/unban")
    suspend fun unbanUser(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    /** Sletter en bruker permanent (kun for admin) */
    @DELETE("users/{email}")
    suspend fun deleteUser(
        @Path("email") email: String,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    // Klubbforespørsel-endepunkter
    /** Sender inn forespørsel om å opprette ny klubb */
    @POST("api/club-requests")
    suspend fun submitClubRequest(
        @Body request: SubmitClubRequestDto,
        @Header("Authorization") token: String
    ): Response<ClubRequestResponseDto>

    // Admin-endepunkter - Klubbforespørsler
    /** Henter alle ventende klubbforespørsler (kun for admin) */
    @GET("api/club-requests")
    suspend fun getClubRequests(
        @Header("Authorization") token: String
    ): Response<List<ClubRequestDto>>

    /** Godkjenner en klubbforespørsel (kun for admin) */
    @POST("api/club-requests/{id}/approve")
    suspend fun approveClubRequest(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    /** Avviser en klubbforespørsel (kun for admin) */
    @POST("api/club-requests/{id}/reject")
    suspend fun rejectClubRequest(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    // Klubb-endepunkter
    /** Henter alle klubber */
    @GET("api/clubs")
    suspend fun getAllClubs(
        @Header("Authorization") token: String
    ): Response<List<ClubDto>>

    /** Henter informasjon om en spesifikk klubb */
    @GET("api/clubs/{clubId}")
    suspend fun getClubById(
        @Path("clubId") clubId: Int,
        @Header("Authorization") token: String
    ): Response<ClubDto>

    // Klubbmedlemsforespørsel-endepunkter
    /** Sender inn forespørsel om å bli medlem av en klubb */
    @POST("api/club-join-requests")
    suspend fun submitClubJoinRequest(
        @Body request: SubmitClubJoinRequestDto,
        @Header("Authorization") token: String
    ): Response<ClubJoinRequestResponseDto>

    /** Henter alle medlemsforespørsler for en klubb (kun for klubbadmin) */
    @GET("api/club-join-requests/club/{clubId}")
    suspend fun getClubJoinRequests(
        @Path("clubId") clubId: Int,
        @Header("Authorization") token: String
    ): Response<List<ClubJoinRequestDto>>

    /** Godkjenner en medlemsforespørsel (kun for klubbadmin) */
    @POST("api/club-join-requests/{id}/approve")
    suspend fun approveClubJoinRequest(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    /** Avviser en medlemsforespørsel (kun for klubbadmin) */
    @POST("api/club-join-requests/{id}/reject")
    suspend fun rejectClubJoinRequest(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<AdminResponseDto>

    // Klubbside-endepunkter
    /** Henter komplett klubbside med kunngjøringer og arrangementer */
    @GET("api/club-page/{clubId}")
    suspend fun getClubPage(
        @Path("clubId") clubId: Int,
        @Header("Authorization") token: String
    ): Response<ClubPageDto>

    /** Oppretter ny kunngjøring på klubbsiden (kun for klubbadmin) */
    @POST("api/club-page/{clubId}/announcements")
    suspend fun createAnnouncement(
        @Path("clubId") clubId: Int,
        @Body request: CreateAnnouncementRequest,
        @Header("Authorization") token: String
    ): Response<ClubAnnouncementDto>

    /** Sletter en kunngjøring (kun for klubbadmin) */
    @DELETE("api/club-page/{clubId}/announcements/{announcementId}")
    suspend fun deleteAnnouncement(
        @Path("clubId") clubId: Int,
        @Path("announcementId") announcementId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    /** Oppretter nytt arrangement for klubben (kun for klubbadmin) */
    @POST("api/club-page/{clubId}/events")
    suspend fun createEvent(
        @Path("clubId") clubId: Int,
        @Body request: CreateEventRequest,
        @Header("Authorization") token: String
    ): Response<ClubEventDto>

    /** Sletter et arrangement (kun for klubbadmin) */
    @DELETE("api/club-page/{clubId}/events/{eventId}")
    suspend fun deleteEvent(
        @Path("clubId") clubId: Int,
        @Path("eventId") eventId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    /** Melder seg på et arrangement */
    @POST("api/club-page/{clubId}/events/{eventId}/join")
    suspend fun joinEvent(
        @Path("clubId") clubId: Int,
        @Path("eventId") eventId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    /** Melder seg av et arrangement */
    @DELETE("api/club-page/{clubId}/events/{eventId}/join")
    suspend fun leaveEvent(
        @Path("clubId") clubId: Int,
        @Path("eventId") eventId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    /** Henter liste over deltakere i et arrangement */
    @GET("api/club-page/{clubId}/events/{eventId}/participants")
    suspend fun getEventParticipants(
        @Path("clubId") clubId: Int,
        @Path("eventId") eventId: Int,
        @Header("Authorization") token: String
    ): Response<List<EventParticipantDto>>

    // FCM Token-registrering
    /** Registrerer FCM-token for push-notifikasjoner */
    @POST("api/notifications/register-token")
    suspend fun registerFcmToken(
        @Body request: RegisterFcmTokenRequest,
        @Header("Authorization") token: String
    ): Response<FcmResponse>
}

