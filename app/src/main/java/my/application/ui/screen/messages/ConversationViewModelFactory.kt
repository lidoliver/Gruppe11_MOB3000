/**
 * Factory for å lage ConversationViewModel med kontakt-e-post som parameter
 */
package my.application.ui.screen.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ConversationViewModelFactory(
    private val contactEmail: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConversationViewModel(contactEmail) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

