package my.application.data.model

data class Player(
    val email: String = "",
    val name: String = email.substringBefore("@"),
    val isHost: Boolean = false,
    val joinedAt: Long = 0,
    val ready: Boolean = false,
    val scores: Map<String, ScoreEntry> = emptyMap()
)

data class ScoreEntry(
    val score: Int = 0,
    val timestamp: Long = 0
)
