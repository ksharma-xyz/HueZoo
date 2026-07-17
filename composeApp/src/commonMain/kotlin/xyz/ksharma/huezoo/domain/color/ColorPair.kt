package xyz.ksharma.huezoo.domain.color

import androidx.compose.ui.graphics.Color

/**
 * A pair of colors for the Color Memory Match game.
 *
 * @param a        The memorised color (Chamber A — shown first, then sealed).
 * @param b        The recall color (Chamber B — the player judges same/different).
 * @param deltaE   Target CIEDE2000 distance between [a] and [b]. `0` when [isSame].
 * @param isSame   True when [a] and [b] are identical — the truth the player must call.
 */
data class ColorPair(
    val a: Color,
    val b: Color,
    val deltaE: Float,
    val isSame: Boolean,
)
