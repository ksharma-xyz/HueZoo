package xyz.ksharma.huezoo.ui.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Singleton observable state for the current player.
 * [gems] is backed by Compose snapshot state so any composable reading it
 * will recompose automatically when a ViewModel calls [updateGems].
 */
class PlayerState {
    var gems: Int by mutableIntStateOf(0)
        private set

    fun updateGems(newGems: Int) {
        gems = newGems
    }
}
