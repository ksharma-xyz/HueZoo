package xyz.ksharma.huezoo.ui.result.state

import xyz.ksharma.huezoo.navigation.GemAward

sealed interface ResultUiState {

    data object Loading : ResultUiState

    data class Ready(
        val gameId: String,
        val deltaE: Float,
        val roundsSurvived: Int,
        val score: Int,
        val isNewPersonalBest: Boolean,
        val personalBestDeltaE: Float?,
        /** Gems earned in this session — shown as "+N gems" on the result screen. */
        val gemsEarned: Int = 0,
        /** Per-line gem breakdown shown staggered on the result screen. */
        val gemBreakdown: List<GemAward> = emptyList(),
        /**
         * For Threshold: true when at least one attempt remains in the current window.
         * For Daily: always false — once per day is the rule.
         */
        val canPlayAgain: Boolean = false,
    ) : ResultUiState
}
