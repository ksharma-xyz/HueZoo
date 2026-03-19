package xyz.ksharma.huezoo.ui.model

import androidx.compose.ui.graphics.Color

/**
 * UI representation of a single swatch shown during a game round.
 *
 * Lives only in the UI layer (ViewModels → Screens). Never passed to domain or data code.
 */
data class SwatchUiModel(
    val color: Color,
    val displayState: SwatchDisplayState = SwatchDisplayState.Default,
)

enum class SwatchDisplayState {
    Default,
    Correct,
    Wrong,
    Revealed,
}
