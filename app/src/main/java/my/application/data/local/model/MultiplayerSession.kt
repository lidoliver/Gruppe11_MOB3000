package my.application.data.model

/**
 * Representerer en multiplayer-spilløkt
 * Inneholder all informasjon om pågående spill med flere spillere
 * @param sessionId Unik identifikator for økten
 * @param baneId ID til banen som spilles
 * @param baneName Navnet på banen
 * @param hostEmail E-postadressen til spilleren som opprettet økten
 * @param status Øktens status: "waiting" (venter på spillere), "in_progress" (pågår), "completed" (ferdig)
 * @param currentHole Nåværende hull som spilles (1-18)
 * @param createdAt Tidspunkt når økten ble opprettet (Unix timestamp)
 * @param players Map med alle spillere i økten (nøkkel: e-postadresse)
 */
data class MultiplayerSession(
    val sessionId: String = "",
    val baneId: String = "",
    val baneName: String = "",
    val hostEmail: String = "",
    val status: String = "waiting",
    val currentHole: Int = 1,
    val createdAt: Long = 0,
    val players: Map<String, Player> = emptyMap()
)





