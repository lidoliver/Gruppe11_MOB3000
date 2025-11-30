package my.application.data.repository

import android.util.Log
import my.application.data.remote.BackendApi
import my.application.data.remote.dto.ConversationDto
import my.application.data.remote.dto.MessageDto
import my.application.data.remote.dto.SendMessageRequest

/**
 * Repository for meldings-relaterte operasjoner
 * Håndterer samtaler og meldinger mellom brukere
 * Brukes av meldingsfunksjonen i appen
 */
class MessagesRepository {
    private val api = BackendApi.service

    /**
     * Henter alle samtaler for innlogget bruker
     * @param token Autentiseringstoken (JWT)
     * @return Result med liste over ConversationDto ved suksess
     */
    suspend fun getConversations(token: String): Result<List<ConversationDto>> {
        return try {
            val response = api.getConversations("Bearer $token")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to get conversations: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("MessagesRepository", "Get conversations error", e)
            Result.failure(e)
        }
    }

    /**
     * Henter alle meldinger i en spesifikk samtale
     * Logger detaljert informasjon om meldinger for debugging
     * @param contactEmail E-postadressen til kontakten
     * @param token Autentiseringstoken (JWT)
     * @return Result med liste over MessageDto ved suksess
     */
    suspend fun getMessages(contactEmail: String, token: String): Result<List<MessageDto>> {
        return try {
            Log.d("MessagesRepository", "Fetching messages for: $contactEmail")
            val response = api.getMessages(contactEmail, "Bearer $token")
            Log.d("MessagesRepository", "Response code: ${response.code()}")
            if (response.isSuccessful) {
                val messages = response.body() ?: emptyList()
                Log.d("MessagesRepository", "Received ${messages.size} messages from API")
                messages.forEachIndexed { index, msg ->
                    Log.d("MessagesRepository", "Message $index: from=${msg.senderEmail}, content=${msg.content.take(20)}")
                }
                Result.success(messages)
            } else {
                val error = "Failed to get messages: ${response.code()} ${response.message()}"
                Log.e("MessagesRepository", error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e("MessagesRepository", "Get messages error", e)
            Result.failure(e)
        }
    }

    /**
     * Sender en melding til en annen bruker
     * @param receiverEmail E-postadressen til mottakeren
     * @param content Meldingsteksten
     * @param token Autentiseringstoken (JWT)
     * @return Result med Unit ved vellykket sending
     */
    suspend fun sendMessage(receiverEmail: String, content: String, token: String): Result<Unit> {
        return try {
            val response = api.sendMessage(
                SendMessageRequest(receiverEmail, content),
                "Bearer $token"
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send message: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("MessagesRepository", "Send message error", e)
            Result.failure(e)
        }
    }
}

