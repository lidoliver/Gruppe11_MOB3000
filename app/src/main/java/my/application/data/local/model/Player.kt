package my.application.data.model

/**
 * Representerer en spiller i et multiplayer-spill
 * @param email Spillerens e-postadresse (unik identifikator)
 * @param name Spillerens visningsnavn (default: del av e-post før @)
 * @param isHost Om spilleren er vert for økten
 * @param joinedAt Tidspunkt når spilleren ble med (Unix timestamp)
 * @param ready Om spilleren er klar til å starte spillet
 * @param scores Map med poengsum for hvert hull (nøkkel: hullnummer)
 */
data class Player(
    val email: String = "",
    val name: String = email.substringBefore("@"),
    val isHost: Boolean = false,
    val joinedAt: Long = 0,
    val ready: Boolean = false,
    val scores: Map<String, ScoreEntry> = emptyMap()
)

/**
 * Representerer en poengsum for et spesifikt hull
 * @param score Antall kast på hullet
 * @param timestamp Tidspunkt når poengsummen ble registrert (Unix timestamp)
 */
data class ScoreEntry(
    val score: Int = 0,
    val timestamp: Long = 0
)
