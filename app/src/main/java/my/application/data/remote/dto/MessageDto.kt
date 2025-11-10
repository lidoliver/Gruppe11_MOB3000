package my.application.data.remote.dto

import com.squareup.moshi.Json

/**
 * Data transfer object for conversation data from backend
 */
data class ConversationDto(
    @Json(name = "contact_email")
    val contactEmail: String,

    @Json(name = "last_message_time")
    val lastMessageTime: String,

    @Json(name = "unread_count")
    val unreadCount: Int,

    @Json(name = "last_message")
    val lastMessage: String?,

    @Json(name = "contact_username")
    val contactUsername: String
)

/**
 * Data transfer object for message data from backend
 */
data class MessageDto(
    @Json(name = "message_id")
    val messageId: Int,

    @Json(name = "sender_email")
    val senderEmail: String,

    @Json(name = "receiver_email")
    val receiverEmail: String,

    @Json(name = "content")
    val content: String,

    @Json(name = "is_read")
    val isRead: Boolean,

    @Json(name = "created_at")
    val createdAt: String,

    @Json(name = "sender_username")
    val senderUsername: String
)

/**
 * Request body for sending a message
 */
data class SendMessageRequest(
    @Json(name = "receiverEmail")
    val receiverEmail: String,

    @Json(name = "content")
    val content: String
)

/**
 * Response for sending a message
 */
data class SendMessageResponse(
    @Json(name = "message_id")
    val messageId: Int,

    @Json(name = "sender_email")
    val senderEmail: String,

    @Json(name = "receiver_email")
    val receiverEmail: String,

    @Json(name = "content")
    val content: String,

    @Json(name = "created_at")
    val createdAt: String
)

