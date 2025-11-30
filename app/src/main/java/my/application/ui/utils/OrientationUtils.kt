package my.application.ui.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Returnerer true hvis enheten er i landskapsmodus
 */
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

/**
 * Tilstandsholder for å håndtere UI-synlighet i landskapsmodus
 */
class LandscapeUiState {
    var isTopBarVisible by mutableStateOf(false)
    var isBottomBarVisible by mutableStateOf(false)

    fun toggleVisibility() {
        isTopBarVisible = !isTopBarVisible
        isBottomBarVisible = !isBottomBarVisible
    }

    fun show() {
        isTopBarVisible = true
        isBottomBarVisible = true
    }

    fun hide() {
        isTopBarVisible = false
        isBottomBarVisible = false
    }
}

@Composable
fun rememberLandscapeUiState(): LandscapeUiState {
    return remember { LandscapeUiState() }
}

