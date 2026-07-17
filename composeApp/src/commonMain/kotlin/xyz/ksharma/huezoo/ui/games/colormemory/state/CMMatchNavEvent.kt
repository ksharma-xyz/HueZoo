package xyz.ksharma.huezoo.ui.games.colormemory.state

sealed interface CMMatchNavEvent {
    data object NavigateToResult : CMMatchNavEvent
}
