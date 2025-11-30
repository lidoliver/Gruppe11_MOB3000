package my.application.data.messaging

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import my.application.data.remote.dto.ConversationDto
import my.application.data.remote.dto.MessageDto
import my.application.data.repository.MessagesRepository
import my.application.data.repository.UserRepository
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Datamodeller for meldingsfunksjonen
 * Populeres fra databasen via backend API
 */

/**
 * Representerer en samtale/kontakt i meldingslisten
 * @param id Unik identifikator for samtalen
 * @param contactId ID til kontaktpersonen
 * @param contactEmail E-postadresse til kontaktpersonen
 * @param contactName Visningsnavn for kontaktpersonen
 * @param lastMessagePreview Forhåndsvisning av siste melding
 * @param timestamp Tidspunkt for siste melding
 * @param isRead Om samtalen har uleste meldinger
 * @param avatarColor Farge for avataren (genereres fra e-post)
 */
data class Conversation(
    val id: Int,
    val contactId: Int,
    val contactEmail: String,
    val contactName: String,
    val lastMessagePreview: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val avatarColor: Long
)

/**
 * Representerer en individuell melding i en samtale
 * @param id Unik identifikator for meldingen
 * @param conversationId ID til samtalen meldingen tilhører
 * @param senderId ID til avsenderen
 * @param text Meldingsteksten
 * @param timestamp Tidspunkt når meldingen ble sendt
 * @param isFromMe Om meldingen er sendt av innlogget bruker
 * @param isRead Om meldingen er lest
 */
data class ChatMessage(
    val id: Int,
    val conversationId: Int,
    val senderId: Int,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val isRead: Boolean = true
)

/**
 * Repository for meldingshåndtering med database-integrasjon
 * Håndterer henting og sending av meldinger via backend API
 */
class MessagingRepository(
    private val messagesRepository: MessagesRepository = MessagesRepository(),
    private val userRepository: UserRepository = UserRepository.getInstance()
) {

    // Enkel krypteringsnøkkel - I produksjon, bruk Android Keystore
    private val ENCRYPTION_KEY = "MySecureKey12345" // 16 tegn for AES-128

    /**
     * Henter alle samtaler for nåværende bruker
     * @return Flow med liste over samtaler
     */
    fun getConversations(): Flow<List<Conversation>> = flow {
        val token = userRepository.getAuthToken()
        if (token == null) {
            Log.e("MessagingRepository", "No auth token available")
            emit(emptyList())
            return@flow
        }

        val cleanToken = token.removePrefix("Bearer ")
        messagesRepository.getConversations(cleanToken)
            .onSuccess { conversationsDto ->
                emit(conversationsDto.map { it.toConversation() })
            }
            .onFailure { exception ->
                Log.e("MessagingRepository", "Error loading conversations", exception)
                emit(emptyList())
            }
    }

    /**
     * Henter alle meldinger for en spesifikk samtale basert på kontaktens e-post
     * @param contactEmail E-postadressen til kontakten
     * @return Flow med liste over meldinger
     */
    fun getMessages(contactEmail: String): Flow<List<ChatMessage>> = flow {
        val token = userRepository.getAuthToken()
        val currentUserEmail = userRepository.getUserEmail()

        Log.d("MessagingRepository", "Loading messages for $contactEmail")
        Log.d("MessagingRepository", "Auth token available: ${token != null}, User email: $currentUserEmail")

        if (token == null || currentUserEmail == null) {
            Log.e("MessagingRepository", "No auth token or user email available")
            emit(emptyList())
            return@flow
        }

        val cleanToken = token.removePrefix("Bearer ")
        messagesRepository.getMessages(contactEmail, cleanToken)
            .onSuccess { messagesDto ->
                Log.d("MessagingRepository", "Received ${messagesDto.size} messages from API")
                val chatMessages = messagesDto.map { it.toChatMessage(currentUserEmail, contactEmail) }
                Log.d("MessagingRepository", "Emitting ${chatMessages.size} chat messages")
                emit(chatMessages)
            }
            .onFailure { exception ->
                Log.e("MessagingRepository", "Error loading messages", exception)
                emit(emptyList())
            }
    }

    /**
     * Sender en melding til en kontakt
     * @param contactEmail E-postadressen til mottakeren
     * @param text Meldingsteksten
     * @return Result med ChatMessage ved suksess
     */
    suspend fun sendMessage(contactEmail: String, text: String): Result<ChatMessage> {
        val token = userRepository.getAuthToken()
        val currentUserEmail = userRepository.getUserEmail()

        if (token == null || currentUserEmail == null) {
            return Result.failure(Exception("Not authenticated"))
        }

        val cleanToken = token.removePrefix("Bearer ")
        return messagesRepository.sendMessage(contactEmail, text, cleanToken)
            .map {
                // Merk: Backend returnerer ikke full melding, så vi oppretter en midlertidig
                ChatMessage(
                    id = 0, // Vil bli erstattet ved oppdatering
                    conversationId = contactEmail.hashCode(),
                    senderId = currentUserEmail.hashCode(),
                    text = text,
                    timestamp = ZonedDateTime.now().toString(),
                    isFromMe = true,
                    isRead = true
                )
            }
    }

    /**
     * Markerer meldinger som lest
     * Håndteres automatisk av backend ved henting av meldinger
     * @param contactEmail E-postadressen til kontakten
     */
    suspend fun markMessagesAsRead(contactEmail: String): Result<Unit> {
        val token = userRepository.getAuthToken()
        if (token == null) {
            return Result.failure(Exception("Not authenticated"))
        }

        val cleanToken = token.removePrefix("Bearer ")
        return messagesRepository.getMessages(contactEmail, cleanToken)
            .map { Unit }
    }

    /**
     * Krypterer en melding (plassholder for fremtidig ende-til-ende kryptering)
     * Returnerer for øyeblikket klartekst siden backend ikke støtter kryptering ennå
     * @param text Meldingsteksten som skal krypteres
     * @return Kryptert tekst eller klartekst ved feil
     */
    fun encryptMessage(text: String): String {
        return try {
            val key = SecretKeySpec(ENCRYPTION_KEY.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(text.toByteArray())
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            Log.e("MessagingRepository", "Encryption error, returning plain text", e)
            text // Fallback til klartekst
        }
    }

    /**
     * Dekrypterer en melding (plassholder for fremtidig ende-til-ende kryptering)
     * Returnerer for øyeblikket klartekst siden backend ikke støtter kryptering ennå
     * @param encryptedText Kryptert meldingstekst
     * @return Dekryptert tekst eller original tekst ved feil
     */
    fun decryptMessage(encryptedText: String): String {
        return try {
            val key = SecretKeySpec(ENCRYPTION_KEY.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decoded = Base64.getDecoder().decode(encryptedText)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted)
        } catch (e: Exception) {
            Log.e("MessagingRepository", "Decryption error, returning as-is", e)
            encryptedText // Fallback til å returnere som den er
        }
    }

    // Konverteringsfunksjoner

    private fun ConversationDto.toConversation(): Conversation {
        return Conversation(
            id = contactEmail.hashCode(),
            contactId = contactEmail.hashCode(),
            contactEmail = contactEmail,
            contactName = contactUsername,
            lastMessagePreview = lastMessage ?: "No messages yet",
            timestamp = formatTimestamp(lastMessageTime),
            isRead = unreadCount == 0,
            avatarColor = generateColorFromEmail(contactEmail)
        )
    }

    private fun MessageDto.toChatMessage(currentUserEmail: String, contactEmail: String): ChatMessage {
        return ChatMessage(
            id = messageId,
            conversationId = contactEmail.hashCode(),
            senderId = senderEmail.hashCode(),
            text = content,
            timestamp = formatTimestamp(createdAt),
            isFromMe = senderEmail == currentUserEmail,
            isRead = isRead
        )
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val dateTime = ZonedDateTime.parse(timestamp)
            val now = ZonedDateTime.now()

            val daysBetween = ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate())

            when {
                daysBetween == 0L -> dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                daysBetween == 1L -> "I går"
                daysBetween < 7 -> "$daysBetween dager siden"
                else -> dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }
        } catch (e: Exception) {
            Log.e("MessagingRepository", "Error parsing timestamp: $timestamp", e)
            timestamp
        }
    }

    private fun generateColorFromEmail(email: String): Long {
        val colors = listOf(
            0xFF6750A4,
            0xFF625B71,
            0xFF7D5260,
            0xFF006874,
            0xFF8E4585,
            0xFF4A5E3F,
            0xFF7C4DFF,
            0xFF00BFA5,
            0xFFFF6F00
        )
        val hash = email.hashCode()
        return colors[(hash.absoluteValue % colors.size)]
    }

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this
}

