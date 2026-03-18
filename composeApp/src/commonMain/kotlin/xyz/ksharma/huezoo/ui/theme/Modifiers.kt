package xyz.ksharma.huezoo.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val GLOW_LAYERS = 8
private const val SHADOW_LAYERS = 4
private const val GLOW_ALPHA_MAX = 0.5f
private const val SHADOW_ALPHA_SCALE = 0.5f
private const val SHADOW_SPREAD_SCALE = 0.25f
private const val SHADOW_OFFSET_SCALE = 0.6f
private const val SHADOW_HEIGHT_SCALE = 0.5f

/**
 * Draws a layered color glow behind the composable.
 * Uses concentric semi-transparent rects — no platform-specific blur needed.
 *
 * NOTE: ensure the parent does not clip (the default for Row/Column/Box), so the
 * glow extending beyond the composable's bounds remains visible. Add padding
 * equal to [glowRadius] on the parent if the glow is cut off.
 */
fun Modifier.colorGlow(
    color: Color,
    glowRadius: Dp = 12.dp,
    cornerRadius: Dp = 16.dp,
): Modifier = drawBehind {
    val glowPx = glowRadius.toPx()
    val cornerPx = cornerRadius.toPx()
    repeat(GLOW_LAYERS) { i ->
        val fraction = (i + 1).toFloat() / GLOW_LAYERS.toFloat()
        val spread = glowPx * fraction
        val t = 1f - fraction
        val alpha = GLOW_ALPHA_MAX * t * t
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(-spread / 2f, -spread / 2f),
            size = Size(size.width + spread, size.height + spread),
            cornerRadius = CornerRadius(cornerPx + spread / 2f),
        )
    }
}

/**
 * Draws a soft offset shadow below the composable for a depth / 3-D feel.
 */
fun Modifier.depthShadow(
    color: Color = Color.Black.copy(alpha = 0.35f),
    offsetY: Dp = 4.dp,
    cornerRadius: Dp = 16.dp,
): Modifier = drawBehind {
    val offsetPx = offsetY.toPx()
    val cornerPx = cornerRadius.toPx()
    repeat(SHADOW_LAYERS) { i ->
        val fraction = (i + 1).toFloat() / SHADOW_LAYERS.toFloat()
        val alpha = color.alpha * fraction * SHADOW_ALPHA_SCALE
        val spread = offsetPx * fraction * SHADOW_SPREAD_SCALE
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(-spread / 2f, offsetPx * fraction * SHADOW_OFFSET_SCALE),
            size = Size(size.width + spread, size.height + spread * SHADOW_HEIGHT_SCALE),
            cornerRadius = CornerRadius(cornerPx),
        )
    }
}
