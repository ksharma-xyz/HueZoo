package xyz.ksharma.huezoo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Transparent gradient overlay that adds a top-left highlight and a bottom-right
 * shadow, giving a color swatch the look of a physical painted chip.
 *
 * Layer this on top of the swatch's colored background.
 */
@Composable
fun SwatchGradientOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.18f),
                        0.45f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.22f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            ),
    )
}
