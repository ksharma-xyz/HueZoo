package xyz.ksharma.huezoo.ui.games.threshold.state

import xyz.ksharma.huezoo.navigation.Result

/** One-shot navigation commands emitted by [ThresholdViewModel] via SharedFlow. */
sealed interface ThresholdNavEvent {
    data class NavigateToResult(val result: Result) : ThresholdNavEvent
    data object NavigateBack : ThresholdNavEvent
}
