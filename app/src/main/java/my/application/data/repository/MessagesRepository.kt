package my.application.data.repository

import android.util.Log
import my.application.data.remote.BackendApi
import my.application.data.remote.dto.ConversationDto
import my.application.data.remote.dto.MessageDto
import my.application.data.remote.dto.SendMessageRequest

class MessagesRepository {
    private val api = BackendApi.service

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

    suspend fun getMessages(contactEmail: String, token: String): Result<List<MessageDto>> {
        return try {
            val response = api.getMessages(contactEmail, "Bearer $token")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to get messages: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("MessagesRepository", "Get messages error", e)
            Result.failure(e)
        }
    }

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

