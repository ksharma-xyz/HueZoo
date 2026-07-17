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
    /** Color Memory Match only — total session score (−50..100). 0 for other games. */
    val score: Int = 0,
    /** Color Memory Match only — longest run of consecutive correct answers. */
    val longestStreak: Int = 0,
    /** Non-null when the player levelled up during this session. */
    val levelUpTo: PlayerLevel? = null,
    /** True when the player correctly identified at MIN_DELTA_E (the perception wall). */
    val hitPerceptionWall: Boolean = false,
)
