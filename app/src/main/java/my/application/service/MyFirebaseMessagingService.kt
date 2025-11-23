package my.application.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import my.application.MainActivity
import my.application.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "default_channel"
        private const val CHANNEL_NAME = "Default Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // TODO: Send this token to your backend server
        // You can use your existing API to register the token
        // e.g., api.registerFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        // Check if message contains a notification payload
        message.notification?.let { notification ->
            Log.d(TAG, "Notification Title: ${notification.title}")
            Log.d(TAG, "Notification Body: ${notification.body}")

            showNotification(
                title = notification.title ?: "New Message",
                body = notification.body ?: "",
                data = message.data
            )
        }

        // Check if message contains a data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${message.data}")

            // Handle data payload
            handleDataPayload(message.data)
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        // Handle different types of messages based on your app's needs
        when (data["type"]) {
            "message" -> {
                // Handle chat message
                val senderEmail = data["senderEmail"]
                val senderName = data["senderName"]
                val messageText = data["message"]

                showNotification(
                    title = senderName ?: "New Message",
                    body = messageText ?: "",
                    data = data,
                    notificationType = "message",
                    targetId = senderEmail
                )
            }
            "friend_request" -> {
                // Handle friend request
                val senderEmail = data["senderEmail"]
                val senderName = data["senderName"]

                showNotification(
                    title = "New Friend Request",
                    body = "$senderName wants to be your friend",
                    data = data,
                    notificationType = "friend_request",
                    targetId = senderEmail
                )
            }
            "club_invite" -> {
                // Handle club invitation
                val clubName = data["clubName"]
                val inviterName = data["inviterName"]

                showNotification(
                    title = "Club Invitation",
                    body = "$inviterName invited you to join $clubName",
                    data = data,
                    notificationType = "club_invite",
                    targetId = clubName
                )
            }
            else -> {
                // Generic notification
                showNotification(
                    title = data["title"] ?: "Notification",
                    body = data["body"] ?: "",
                    data = data
                )
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        notificationType: String? = null,
        targetId: String? = null
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for messages and updates"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create an intent to open the app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add extra data to navigate to specific screen
            notificationType?.let { putExtra("notification_type", it) }
            targetId?.let { putExtra("target_id", it) }
            data["senderEmail"]?.let { putExtra("sender_email", it) }
            data["senderName"]?.let { putExtra("sender_name", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Replace with your app icon
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

