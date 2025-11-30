package my.application.data.remote.api

import my.application.data.remote.dto.CourseGameHistory
import my.application.data.remote.dto.PlayerStats
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit API for spiller-relaterte endepunkter
 * Definerer HTTP-kall som henter spillerdata fra backend
 * Henter statistikk for spillere, identifisert med e-post
 */
interface PlayerApi {
    /**
     * Henter spillerstatistikk for en bruker
     * @param email Spillerens e-postadresse
     */
    @GET("api/players/{email}/stats")
    suspend fun getPlayerStats(@Path("email", encoded = true) email: String): PlayerStats

    /**
     * Henter spillhistorikk for en spesifikk bane
     * @param email Spillerens e-postadresse
     * @param courseName Navnet på banen
     */
    @GET("api/players/{email}/courses/{courseName}/games")
    suspend fun getPlayerCourseGames(
        @Path("email", encoded = true) email: String,
        @Path("courseName", encoded = true) courseName: String
    ): CourseGameHistory
}
