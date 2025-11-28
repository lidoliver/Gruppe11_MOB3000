package my.application.data.model



data class MultiplayerSession(
    val sessionId: String = "",
    val baneId: String = "",
    val baneName: String = "",
    val hostEmail: String = "",
    val status: String = "waiting",
    val currentHole: Int = 1,
    val createdAt: Long = 0,
    val players: Map<String, Player> = emptyMap() // Changed from List to Map
)





