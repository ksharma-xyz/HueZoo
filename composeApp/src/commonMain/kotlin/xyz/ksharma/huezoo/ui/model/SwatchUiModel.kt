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
    /**
     * DEBUG ONLY — always false in release builds.
     *
     * Set to true on the odd (correct) swatch by [ThresholdViewModel] when
     * [PlatformOps.isDebugBuild] is true. Causes [RadialSwatchLayout] to render
     * a dim white border on that tile so testers can verify the correct answer
     * without guessing. Never visible in production.
     */
    val isDebugOdd: Boolean = false,
)

enum class SwatchDisplayState {
    Default,
    Correct,
    Wrong,
    Revealed,
}
