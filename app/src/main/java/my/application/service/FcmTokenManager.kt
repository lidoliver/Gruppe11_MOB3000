package my.application.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Håndterer Firebase Cloud Messaging (FCM) tokens
 * Administrerer lagring, henting og abonnement på push-varsler
 */
object FcmTokenManager {
    private const val TAG = "FcmTokenManager"
    private const val PREFS_NAME = "fcm_prefs"
    private const val KEY_FCM_TOKEN = "fcm_token"
    const val PERMISSION_REQUEST_CODE = 1001

    /**
     * Henter gjeldende FCM-token fra Firebase
     * @return FCM-token som String, eller null ved feil
     */
    suspend fun getToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM Token: $token")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token", e)
            null
        }
    }

    /**
     * Lagrer FCM-token til SharedPreferences
     * @param context Applikasjonskontekst
     * @param token FCM-tokenet som skal lagres
     */
    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    /**
     * Henter lagret FCM-token fra SharedPreferences
     * @param context Applikasjonskontekst
     * @return Lagret FCM-token, eller null hvis ikke funnet
     */
    fun getSavedToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Sjekker om varslingstillatelse er gitt
     * @param context Applikasjonskontekst
     * @return true hvis tillatelse er gitt, false ellers
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Tillatelse ikke nødvendig for Android 12 og eldre
        }
    }

    /**
     * Be om varslingstillatelse (Android 13+)
     * @param activity Activity-konteksten for tillatelsesdialogen
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    /**
     * Abonnerer på et FCM-emne
     * @param topic Emnenavn
     * @return true ved suksess, false ved feil
     */
    suspend fun subscribeToTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Log.d(TAG, "Subscribed to topic: $topic")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to topic: $topic", e)
            false
        }
    }

    /**
     * Avslutter abonnement på et FCM-emne
     * @param topic Emnenavn
     * @return true ved suksess, false ved feil
     */
    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            Log.d(TAG, "Unsubscribed from topic: $topic")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from topic: $topic", e)
            false
        }
    }
}

