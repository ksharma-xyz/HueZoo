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
        /**
         * For Threshold: true when at least one attempt remains in the current window.
         * For Daily: always false — once per day is the rule.
         */
        val canPlayAgain: Boolean = false,
    ) : ResultUiState
}
