package xyz.ksharma.huezoo.ui.games.daily.state

sealed interface DailyUiEvent {
    data class SwatchTapped(val index: Int) : DailyUiEvent
}
