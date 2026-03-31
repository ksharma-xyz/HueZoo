package xyz.ksharma.huezoo.ui.result.state

import xyz.ksharma.huezoo.navigation.GemAward
import xyz.ksharma.huezoo.ui.model.PlayerLevel

sealed interface ResultUiState {

    data object Loading : ResultUiState

    data class Ready(
        val gameId: String,
        val deltaE: Float,
        val roundsSurvived: Int,
        /** Correct rounds/taps across the full session. */
        val correctRounds: Int = 0,
        /** Total rounds/taps played across the full session. */
        val totalRounds: Int = 0,
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
        /** True when the player has purchased the Unlimited upgrade. */
        val isPaid: Boolean = false,
        /** Non-null when the player levelled up during this session. */
        val levelUpTo: PlayerLevel? = null,
        /**
         * Live price string fetched from the store (e.g. "$2.99", "€2.99").
         * Empty until the store query returns; UI falls back to "UNLOCK FOREVER" without a price.
         */
        val priceLabel: String = "",
    ) : ResultUiState
}
