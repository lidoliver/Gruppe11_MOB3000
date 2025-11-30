package my.application.data.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
import my.application.data.repository.UserRepository

/**
 * Håndterer periodiske heartbeat-oppdateringer til backend for å spore online-status
 * Starter automatisk når appen er i forgrunnen og stopper når den er i bakgrunnen
 * Sender oppdateringer hvert minutt for å holde online-status oppdatert
 */
class HeartbeatManager private constructor(private val application: Application) {

    private val userRepository = UserRepository.getInstance()
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Heartbeat-intervall i millisekunder (1 minutt for rask statusoppdatering)
    private val HEARTBEAT_INTERVAL_MS = 1 * 60 * 1000L

    init {
        // Registrer lifecycle observer for å starte/stoppe heartbeat basert på app-tilstand
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
     * Start sending av periodiske heartbeats til backend
     * Sender umiddelbart første heartbeat, deretter hvert minutt
     */
    fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) {
            Log.d(TAG, "Heartbeat already running")
            return
        }

        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    // Send kun heartbeat hvis bruker er innlogget
                    if (userRepository.getAuthToken() != null) {
                        // Send heartbeat
                        userRepository.updateHeartbeat()
                        Log.d(TAG, "Heartbeat sent successfully")
                    } else {
                        Log.d(TAG, "Skipping heartbeat - user not logged in")
                    }
                } catch (e: Exception) {
                    // Ikke logg som feil siden heartbeat-feil forventes når offline
                    Log.d(TAG, "Heartbeat failed (this is normal if offline): ${e.message}")
                }

                // Vent til neste heartbeat-intervall
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }

        Log.d(TAG, "Heartbeat started - first heartbeat will be sent immediately")
    }

    /**
     * Stopper sending av heartbeats
     */
    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        Log.d(TAG, "Heartbeat stopped")
    }

    /**
     * Send et umiddelbart heartbeat
     * Nyttig etter innlogging eller viktige handlinger
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
     * Rydder opp ressurser
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

