package xyz.ksharma.huezoo.ui.games.daily.state

sealed interface DailyNavEvent {
    data object NavigateToResult : DailyNavEvent
    data object NavigateBack : DailyNavEvent
}
