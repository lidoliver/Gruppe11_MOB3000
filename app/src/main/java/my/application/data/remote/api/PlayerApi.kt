package my.application.data.remote.api

import my.application.data.remote.dto.CourseGameHistory
import my.application.data.remote.dto.PlayerStats
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit API for spiller relaterte endepunkter
 * Definerer HTTP kall som henter spiller data fra backend
 * Henter statistikk for spillere, identifisert med email
 */
interface PlayerApi {
    @GET("api/players/{email}/stats")
    suspend fun getPlayerStats(@Path("email", encoded = true) email: String): PlayerStats

    @GET("api/players/{email}/courses/{courseName}/games")
    suspend fun getPlayerCourseGames(
        @Path("email", encoded = true) email: String,
        @Path("courseName", encoded = true) courseName: String
    ): CourseGameHistory
}
