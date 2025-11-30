package my.application.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * FCM (Firebase Cloud Messaging) API-endepunkter
 * Brukes for push-notifikasjoner til mobile enheter
 * Legg disse til i din eksisterende BackendApiService.kt-interface
 */

/**
 * Request body for registrering av FCM-token
 * Kobler brukerens e-post med deres enhet-token
 */
data class RegisterFcmTokenRequest(
    val userEmail: String,
    val fcmToken: String
)

/**
 * Response fra FCM-operasjoner
 */
data class FcmResponse(
    val success: Boolean,
    val message: String,
    val messageId: String? = null
)

/**
 * LEGG TIL DISSE METODENE I DIN BackendApiService INTERFACE:
 *
 * // FCM Token Registration
 * @POST("api/notifications/register-token")
 * suspend fun registerFcmToken(
 *     @Body request: RegisterFcmTokenRequest,
 *     @Header("Authorization") token: String
 * ): Response<FcmResponse>
 *
 *
 * USAGE EXAMPLE:
 *
 * import my.application.service.FcmTokenManager
 * import androidx.lifecycle.lifecycleScope
 * import kotlinx.coroutines.launch
 *
 * // In your ViewModel or Activity after user logs in:
 * lifecycleScope.launch {
 *     try {
 *         // Get FCM token
 *         val fcmToken = FcmTokenManager.getToken()
 *
 *         if (fcmToken != null) {
 *             // Save locally
 *             FcmTokenManager.saveToken(context, fcmToken)
 *
 *             // Send to backend (use logged-in user's email)
 *             val request = RegisterFcmTokenRequest(
 *                 userEmail = currentUserEmail, // e.g., "user@example.com"
 *                 fcmToken = fcmToken
 *             )
 *
 *             val response = BackendApi.service.registerFcmToken(
 *                 request = request,
 *                 token = "Bearer $authToken"
 *             )
 *
 *             if (response.isSuccessful) {
 *                 Log.d("FCM", "Token registered successfully")
 *             } else {
 *                 Log.e("FCM", "Failed to register token: ${response.code()}")
 *             }
 *         }
 *     } catch (e: Exception) {
 *         Log.e("FCM", "Error registering token", e)
 *     }
 * }
 */

