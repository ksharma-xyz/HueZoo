package xyz.ksharma.huezoo.ui.home.state

sealed interface HomeUiEvent {
    data object ThresholdCardTapped : HomeUiEvent
    data object DailyCardTapped : HomeUiEvent
    data object UnlockTapped : HomeUiEvent
    data object ScreenResumed : HomeUiEvent
    data object DebugResetTapped : HomeUiEvent
}
