package xyz.ksharma.huezoo.ui.model

import androidx.compose.ui.graphics.Color
import xyz.ksharma.huezoo.ui.theme.HuezooColors

/**
 * Five-tier player level system based on lifetime gems earned.
 *
 * Gem earn rates (see GameRewardRates):
 *   Threshold correct tap : +2 gems
 *   Threshold ΔE milestones: +5 / +10 / +25 bonus
 *   Daily correct round   : +5 gems
 *   Daily participation   : +3 gems (any completion)
 *   Daily perfect (6/6)   : +20 bonus gems
 *
 * Approximate sessions to tier-up at ~15 gems/session avg:
 *   Trained ≈ 10 sessions | Sharp ≈ 50 | Elite ≈ 333 | Master ≈ 3 333
 */
enum class PlayerLevel(
    val displayName: String,
    val minGems: Int,
    val levelColor: Color,
    val shelfColor: Color,
) {
    Rookie(
        displayName = "ROOKIE",
        minGems = 0,
        levelColor = HuezooColors.AccentCyan,
        shelfColor = HuezooColors.ShelfCyan,
    ),
    Trained(
        displayName = "TRAINED",
        minGems = 150,
        levelColor = HuezooColors.AccentGreen,
        shelfColor = HuezooColors.ShelfGreen,
    ),
    Sharp(
        displayName = "SHARP",
        minGems = 750,
        levelColor = HuezooColors.AccentMagenta,
        shelfColor = HuezooColors.ShelfMagenta,
    ),
    Elite(
        displayName = "ELITE",
        minGems = 5_000,
        levelColor = HuezooColors.AccentYellow,
        shelfColor = HuezooColors.ShelfYellow,
    ),
    Master(
        displayName = "MASTER",
        minGems = 50_000,
        levelColor = Color(0xFFFFB800),
        shelfColor = HuezooColors.ShelfMaster,
    ),
    ;

    companion object {
        fun fromGems(gems: Int): PlayerLevel = when {
            gems >= 50_000 -> Master
            gems >= 5_000 -> Elite
            gems >= 750 -> Sharp
            gems >= 150 -> Trained
            else -> Rookie
        }
    }
}
