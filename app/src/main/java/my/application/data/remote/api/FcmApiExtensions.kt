package my.application.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * FCM (Firebase Cloud Messaging) API endpoints
 *
 * Add these to your existing BackendApiService.kt interface
 */

// Data classes for FCM requests/responses
data class RegisterFcmTokenRequest(
    val userEmail: String,
    val fcmToken: String
)

data class FcmResponse(
    val success: Boolean,
    val message: String,
    val messageId: String? = null
)

/**
 * ADD THESE METHODS TO YOUR BackendApiService INTERFACE:
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

