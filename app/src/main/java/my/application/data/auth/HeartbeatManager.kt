package my.application.data.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
import my.application.data.repository.UserRepository

/**
 * Manages periodic heartbeat updates to backend for tracking online status
 * Automatically starts when app is in foreground and stops when in background
 */
class HeartbeatManager private constructor(private val application: Application) {

    private val userRepository = UserRepository.getInstance()
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Heartbeat interval in milliseconds (1 minute for faster status updates)
    private val HEARTBEAT_INTERVAL_MS = 1 * 60 * 1000L

    init {
        // Register lifecycle observer to start/stop heartbeat based on app state
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                Log.d(TAG, "App moved to foreground - starting heartbeat")
                startHeartbeat()
            }

            override fun onStop(owner: LifecycleOwner) {
                Log.d(TAG, "App moved to background - stopping heartbeat")
                stopHeartbeat()
            }
        })
    }

    /**
     * Start sending periodic heartbeats to backend
     */
    fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) {
            Log.d(TAG, "Heartbeat already running")
            return
        }

        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    // Only send heartbeat if user is logged in
                    if (userRepository.getAuthToken() != null) {
                        // Send heartbeat
                        userRepository.updateHeartbeat()
                        Log.d(TAG, "Heartbeat sent successfully")
                    } else {
                        Log.d(TAG, "Skipping heartbeat - user not logged in")
                    }
                } catch (e: Exception) {
                    // Don't log as error since heartbeat failures are expected when offline
                    Log.d(TAG, "Heartbeat failed (this is normal if offline): ${e.message}")
                }

                // Wait for next heartbeat interval (moved AFTER sending, not before)
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }

        Log.d(TAG, "Heartbeat started - first heartbeat will be sent immediately")
    }

    /**
     * Stop sending heartbeats
     */
    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        Log.d(TAG, "Heartbeat stopped")
    }

    /**
     * Send an immediate heartbeat (useful after login or important actions)
     */
    suspend fun sendImmediateHeartbeat() {
        try {
            userRepository.updateHeartbeat()
            Log.d(TAG, "Immediate heartbeat sent")
        } catch (e: Exception) {
            Log.d(TAG, "Immediate heartbeat failed: ${e.message}")
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopHeartbeat()
        scope.cancel()
    }

    companion object {
        private const val TAG = "HeartbeatManager"

        @Volatile
        private var instance: HeartbeatManager? = null

        fun getInstance(application: Application): HeartbeatManager {
            return instance ?: synchronized(this) {
                instance ?: HeartbeatManager(application).also { instance = it }
            }
        }
    }
}

