package xyz.ksharma.huezoo.ui.result.state

sealed interface ResultUiState {

    data object Loading : ResultUiState

    data class Ready(
        val gameId: String,
        val deltaE: Float,
        val roundsSurvived: Int,
        val score: Int,
        val isNewPersonalBest: Boolean,
        val personalBestDeltaE: Float?,
    ) : ResultUiState
}
