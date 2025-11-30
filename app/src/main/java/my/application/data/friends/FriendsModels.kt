package my.application.data.friends

/**
 * Datamodeller for vennefunksjonen
 * Domenobjekter brukt gjennom hele appen for å representere vennedata
 */

/**
 * Representerer en venn i vennelisten
 * Domenobjekt populert fra backend API via FriendsRepository
 * @param id Unik identifikator for vennskapet
 * @param userId Bruker-ID
 * @param name Fullt navn på vennen
 * @param username Brukernavn
 * @param email Vennens e-postadresse (brukes for meldinger og API-kall)
 * @param status Online-status (ONLINE, OFFLINE, IN_GAME)
 * @param lastPlayed Siste gang de spilte
 * @param gamesPlayed Antall spill spilt sammen
 * @param avatarColor Farge for avatar (lagret som Long for database-kompatibilitet)
 * @param hasUnreadMessages Om det er uleste meldinger fra vennen
 */
data class Friend(
    val id: Int,
    val userId: Int,
    val name: String,
    val username: String,
    val email: String,
    val status: FriendStatus = FriendStatus.ONLINE,
    val lastPlayed: String? = null,
    val gamesPlayed: Int = 0,
    val avatarColor: Long,
    val hasUnreadMessages: Boolean = false
)

/**
 * Vennens online-status
 */
enum class FriendStatus {
    ONLINE,    // Pålogget og aktiv
    OFFLINE,   // Ikke pålogget
    IN_GAME    // Spiller for øyeblikket
}

/**
 * Spillhistorikk-oppføring for en venn
 * Fremtidig forbedring: Vil bli populert fra spillhistorikkdata
 */
data class PlayHistoryEntry(
    val id: Int,
    val friendId: Int,
    val courseName: String,
    val score: Int,
    val par: Int,
    val date: String,
    val duration: String? = null
)

