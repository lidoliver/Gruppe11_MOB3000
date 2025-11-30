package My.application.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data transfer object for spillerdata
 * Brukes for enkel spillerinformasjon i multiplayer-økter
 */
@JsonClass(generateAdapter = true)
class PlayerDto (
    @Json(name="player_id")
    val playerId: String,

    @Json(name="player_name")
    val playerName: String,

)