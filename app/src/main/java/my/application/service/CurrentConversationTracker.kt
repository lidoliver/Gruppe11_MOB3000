package my.application.service

import android.util.Log

/**
 * Singleton to track which conversation the user is currently viewing
 * This allows us to suppress notifications when the user is already in that conversation
 */
object CurrentConversationTracker {
    private const val TAG = "ConversationTracker"
    
    @Volatile
    private var currentConversationEmail: String? = null
    
    /**
     * Set the email of the user whose conversation is currently being viewed
     */
    fun setCurrentConversation(email: String?) {
        Log.d(TAG, "Current conversation set to: $email")
        currentConversationEmail = email
    }
    
    /**
     * Check if the user is currently viewing a conversation with the given email
     */
    fun isViewingConversationWith(email: String): Boolean {
        val isViewing = currentConversationEmail == email
        Log.d(TAG, "Checking if viewing conversation with $email: $isViewing (current: $currentConversationEmail)")
        return isViewing
    }
    
    /**
     * Clear the current conversation (called when leaving conversation screen)
     */
    fun clearCurrentConversation() {
        Log.d(TAG, "Clearing current conversation")
        currentConversationEmail = null
    }
}
