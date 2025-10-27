package my.application.data.network.dto

import com.squareup.moshi.Json

data class SendFriendRequestDto(
    @Json(name = "friendEmail") val friendEmail: String
)
