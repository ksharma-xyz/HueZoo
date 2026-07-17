package xyz.ksharma.huezoo.ui.games.colormemory.state

sealed interface CMMatchUiEvent {
    /** Player tapped SAME (`saidSame = true`) or DIFFERENT (`saidSame = false`). */
    data class Answer(val saidSame: Boolean) : CMMatchUiEvent
}
