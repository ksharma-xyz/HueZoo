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
    /** Hue (0–360°) of this level's accent color — used to exclude that hue zone from game swatches. */
    val levelHue: Float,
) {
    Rookie(
        displayName = "ROOKIE",
        minGems = 0,
        levelColor = HuezooColors.AccentCyan,
        shelfColor = HuezooColors.ShelfCyan,
        levelHue = 186f,
    ),
    Trained(
        displayName = "TRAINED",
        minGems = 150,
        levelColor = HuezooColors.AccentGreen,
        shelfColor = HuezooColors.ShelfGreen,
        levelHue = 159f,
    ),
    Sharp(
        displayName = "SHARP",
        minGems = 750,
        levelColor = HuezooColors.AccentOrange,
        shelfColor = HuezooColors.ShelfOrange,
        levelHue = 29f,
    ),
    Elite(
        displayName = "ELITE",
        minGems = 5_000,
        levelColor = HuezooColors.AccentPink,
        shelfColor = HuezooColors.ShelfPink,
        levelHue = 330f,
    ),
    Master(
        displayName = "MASTER",
        minGems = 50_000,
        levelColor = HuezooColors.AccentGold,
        shelfColor = HuezooColors.ShelfMaster,
        levelHue = 43f,
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

        /** Bonus gems awarded when the player reaches [level] for the first time. */
        fun levelUpBonus(level: PlayerLevel): Int = when (level) {
            Rookie -> BONUS_ROOKIE
            Trained -> BONUS_TRAINED
            Sharp -> BONUS_SHARP
            Elite -> BONUS_ELITE
            Master -> BONUS_MASTER
        }

        private const val BONUS_ROOKIE = 0
        private const val BONUS_TRAINED = 20
        private const val BONUS_SHARP = 40
        private const val BONUS_ELITE = 60
        private const val BONUS_MASTER = 100
    }
}
