package xyz.ksharma.huezoo.ui.games.threshold.state

sealed interface ThresholdUiEvent {
    /** Player tapped the swatch at [index] (0, 1, or 2). */
    data class SwatchTapped(val index: Int) : ThresholdUiEvent
}
