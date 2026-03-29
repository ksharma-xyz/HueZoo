package xyz.ksharma.huezoo.navigation

import xyz.ksharma.huezoo.ui.model.PlayerLevel

/**
 * In-memory game session result — written by game ViewModels into [SessionResultCache]
 * before navigating to the result screen. Not serialized; lives only in memory.
 */
data class SessionResult(
    val gameId: String,
    val deltaE: Float,
    val roundsSurvived: Int,
    val correctRounds: Int = 0,
    val totalRounds: Int = 0,
    val gemsEarned: Int = 0,
    val gemBreakdown: List<GemAward> = emptyList(),
    /** Non-null when the player levelled up during this session. */
    val levelUpTo: PlayerLevel? = null,
)
