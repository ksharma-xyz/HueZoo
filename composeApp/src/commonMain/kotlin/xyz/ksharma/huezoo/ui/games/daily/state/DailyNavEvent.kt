package xyz.ksharma.huezoo.ui.games.daily.state

import xyz.ksharma.huezoo.navigation.Result

sealed interface DailyNavEvent {
    data class NavigateToResult(val result: Result) : DailyNavEvent
    data object NavigateBack : DailyNavEvent
}
