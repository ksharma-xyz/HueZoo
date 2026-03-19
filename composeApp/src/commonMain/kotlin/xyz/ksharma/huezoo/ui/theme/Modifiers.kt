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
private const val NEON_STRIKE_INNER_DP = 2f
private const val NEON_STRIKE_OUTER_DP = 4f
private const val NEON_STRIKE_OUTER_ALPHA = 0.3f
private const val RIM_LIGHT_ALPHA = 0.10f
private const val RIM_LIGHT_STROKE_DP = 1f

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
 * Draws a hard neon ring around the composable — zero blur, two concentric strokes.
 *
 * - Inner ring: [NEON_STRIKE_INNER_DP]dp solid at full [color] opacity
 * - Outer ring: [NEON_STRIKE_OUTER_DP]dp at [NEON_STRIKE_OUTER_ALPHA] opacity
 *
 * Distinct from [colorGlow] (which is soft/radial). Use for:
 * - Outlier swatch on reveal
 * - Active / selected game card
 * - DeltaEBadge at hard difficulty (ΔE < 1)
 *
 * NOTE: strokes are drawn *outside* the composable bounds. Ensure the parent
 * does not clip, and add padding ≥ [NEON_STRIKE_OUTER_DP]dp if needed.
 */
fun Modifier.neonStrike(
    color: Color,
    cornerRadius: Dp = 16.dp,
): Modifier = drawBehind {
    val cr = CornerRadius(cornerRadius.toPx())
    val inner = NEON_STRIKE_INNER_DP.dp.toPx()
    val outer = NEON_STRIKE_OUTER_DP.dp.toPx()

    // Outer ring — wider, translucent
    drawRoundRect(
        color = color.copy(alpha = NEON_STRIKE_OUTER_ALPHA),
        topLeft = Offset(-outer / 2f, -outer / 2f),
        size = Size(size.width + outer, size.height + outer),
        cornerRadius = CornerRadius(cr.x + outer / 2f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = outer),
    )

    // Inner ring — narrow, fully opaque
    drawRoundRect(
        color = color,
        topLeft = Offset(-inner / 2f, -inner / 2f),
        size = Size(size.width + inner, size.height + inner),
        cornerRadius = CornerRadius(cr.x + inner / 2f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = inner),
    )
}

/**
 * Draws a subtle 1dp inset highlight on the top and left edges of the composable,
 * simulating a light source from the top-left (chamfered metal feel).
 *
 * Use on cards, panels, and stat containers. Apply *after* background so the
 * rim sits on top of the fill.
 */
fun Modifier.rimLight(
    cornerRadius: Dp = 16.dp,
): Modifier = drawBehind {
    val stroke = RIM_LIGHT_STROKE_DP.dp.toPx()
    val cr = cornerRadius.toPx()
    val half = stroke / 2f

    // Top edge
    drawLine(
        color = Color.White.copy(alpha = RIM_LIGHT_ALPHA),
        start = Offset(cr, half),
        end = Offset(size.width - cr, half),
        strokeWidth = stroke,
    )

    // Left edge
    drawLine(
        color = Color.White.copy(alpha = RIM_LIGHT_ALPHA),
        start = Offset(half, cr),
        end = Offset(half, size.height - cr),
        strokeWidth = stroke,
    )
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
