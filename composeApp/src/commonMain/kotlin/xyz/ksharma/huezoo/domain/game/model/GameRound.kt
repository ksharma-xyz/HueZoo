package xyz.ksharma.huezoo.domain.game.model

import androidx.compose.ui.graphics.Color

/**
 * A single game round: three swatches displayed to the player.
 *
 * [swatches] always contains exactly 3 colours — 2 identical (base) and 1 different (odd).
 * [oddIndex] identifies which position (0, 1, or 2) holds the odd colour.
 * [deltaE] is the CIEDE2000 perceptual distance between base and odd.
 */
data class GameRound(
    val swatches: List<Color>,
    val oddIndex: Int,
    val deltaE: Float,
)
