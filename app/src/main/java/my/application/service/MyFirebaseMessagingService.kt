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

        // TODO: Send dette tokenet til backend-serveren din
        // Du kan bruke ditt eksisterende API for å registrere tokenet
        // f.eks., api.registerFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")
        Log.d(TAG, "Has notification payload: ${message.notification != null}")
        Log.d(TAG, "Has data payload: ${message.data.isNotEmpty()}")
        Log.d(TAG, "Data contents: ${message.data}")

        // Prioriter data-innhold for tilpasset håndtering
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Processing data payload")
            handleDataPayload(message.data, message.notification)
        } else if (message.notification != null) {
            // Tilbakefall til varselinnhold hvis ingen data
            Log.d(TAG, "Processing notification payload only")
            message.notification?.let { notification ->
                showNotification(
                    title = notification.title ?: "New Message",
                    body = notification.body ?: "",
                    data = message.data
                )
            }
        } else {
            Log.w(TAG, "Message has neither notification nor data payload!")
        }
    }

    /**
     * Håndterer data-payload basert på meldingstype
     * Støtter: message, friend_request, club_invite
     */
    private fun handleDataPayload(data: Map<String, String>, notification: RemoteMessage.Notification? = null) {
        val dataType = data["type"]
        Log.d(TAG, "Handling data payload with type: $dataType")

        when (dataType) {
            "message" -> {
                // Håndter chatmelding
                val senderEmail = data["senderEmail"]
                val senderName = data["senderName"]
                val messageText = data["message"]

                Log.d(TAG, "Message notification - sender: $senderName, text: ${messageText?.take(20)}")

                showNotification(
                    title = senderName ?: notification?.title ?: "New Message",
                    body = messageText ?: notification?.body ?: "",
                    data = data,
                    notificationType = "message",
                    targetId = senderEmail
                )
            }
            "friend_request" -> {
                // Håndter venneforespørsel
                val senderEmail = data["senderEmail"]
                val senderName = data["senderName"]

                Log.d(TAG, "Friend request notification - sender: $senderName")

                showNotification(
                    title = "New Friend Request",
                    body = "$senderName wants to be your friend",
                    data = data,
                    notificationType = "friend_request",
                    targetId = senderEmail
                )
            }
            "club_invite" -> {
                // Håndter klubbinvitasjon
                val clubName = data["clubName"]
                val inviterName = data["inviterName"]

                Log.d(TAG, "Club invite notification - club: $clubName")

                showNotification(
                    title = "Club Invitation",
                    body = "$inviterName invited you to join $clubName",
                    data = data,
                    notificationType = "club_invite",
                    targetId = clubName
                )
            }
            else -> {
                // Generisk varsel eller tilbakefall til varselinnhold
                Log.d(TAG, "Generic/unknown notification type: $dataType")

                showNotification(
                    title = data["title"] ?: notification?.title ?: "Notification",
                    body = data["body"] ?: notification?.body ?: "",
                    data = data
                )
            }
        }
    }

    /**
     * Viser et lokalt varsel til brukeren
     * @param title Tittelen på varselet
     * @param body Hovedteksten i varselet
     * @param data Ekstra data fra meldingen
     * @param notificationType Type varsel (message, friend_request, club_invite)
     * @param targetId ID til målet (e-post, klubbnavn etc.)
     */
    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        notificationType: String? = null,
        targetId: String? = null
    ) {
        Log.d(TAG, "showNotification called - title: $title, body: ${body.take(30)}, type: $notificationType")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Sjekk om varsler er aktivert
        if (!notificationManager.areNotificationsEnabled()) {
            Log.e(TAG, "❌ Notifications are disabled for this app! User needs to enable in Settings.")
            return
        }

        // Opprett varselkanal for Android O og nyere
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
            Log.d(TAG, "Notification channel created/verified")
        }

        // Opprett en intent for å åpne appen når varselet trykkes
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Legg til ekstra data for å navigere til spesifikk skjerm
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

        // Bygg varselet
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Erstatt med ditt app-ikon
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "✅ Notification displayed with ID: $notificationId")
    }
}

