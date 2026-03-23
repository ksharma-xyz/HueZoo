package xyz.ksharma.huezoo.ui.games.threshold.state

/** One-shot navigation commands emitted by [ThresholdViewModel] via SharedFlow. */
sealed interface ThresholdNavEvent {
    data object NavigateToResult : ThresholdNavEvent
    data object NavigateBack : ThresholdNavEvent
}
