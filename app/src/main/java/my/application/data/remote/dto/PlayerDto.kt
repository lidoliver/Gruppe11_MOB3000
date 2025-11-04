package My.application.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PlayerDto (
    @Json(name="player_id")
    val playerId: String,

    @Json(name="player_name")
    val playerName: String,

)