package xyz.ksharma.huezoo.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Plays a one-shot shimmer celebration over the composable for [durationMs] milliseconds,
 * then stops cleanly. Re-triggers every time [active] flips to `true`.
 *
 * Layers drawn in order:
 *  1. **Radial glow fill** — [glowColor] centered on the composable, fades to transparent at edges.
 *  2. **Diagonal shimmer sweep** — a [shimmerColor] band travels left→right [sweepCount] times.
 *  3. **Bright border** — [glowColor] border traces the composable edges.
 *
 * Everything shares a smooth envelope: fades in over the first 8 % of the timeline and fades
 * out over the last 20 %, so nothing pops on or off.
 *
 * To adjust the look, only change constants in this file:
 *   [GLOW_CENTER_ALPHA], [GLOW_MID_ALPHA], [GLOW_RADIUS_FACTOR],
 *   [SHIMMER_PEAK_ALPHA], [SHIMMER_HALF_WIDTH_PX], [BORDER_ALPHA]
 *
 * @param active        Starts the animation when `true`; resets when `false`.
 * @param glowColor     Color of the radial fill and border ring.
 * @param shimmerColor  Color of the traveling light band. Defaults to warm gold —
 *                      high contrast against magenta/pink glow colors.
 * @param durationMs    Total one-shot duration in milliseconds.
 * @param sweepCount    Number of left→right passes the shimmer band makes in [durationMs].
 */
fun Modifier.shimmerCelebration(
    active: Boolean,
    glowColor: Color,
    shimmerColor: Color = HuezooColors.AccentYellow,
    durationMs: Int = 5_000,
    sweepCount: Int = 4,
): Modifier = composed {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(active) {
        if (active) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing),
            )
        } else {
            progress.snapTo(0f)
        }
    }

    val p = progress.value
    val isAnimating = p > 0f && p < 1f

    // Smooth fade-in / fade-out envelope
    val envelope = when {
        p < ENVELOPE_FADE_IN  -> p / ENVELOPE_FADE_IN
        p > ENVELOPE_FADE_OUT -> (1f - p) / (1f - ENVELOPE_FADE_OUT)
        else -> 1f
    }

    // Shimmer band cycles sweepCount times across the full duration
    val shimmerFraction = (p * sweepCount.toFloat()) % 1f

    drawBehind {
        if (!isAnimating) return@drawBehind

        // 1. Radial glow fill
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to glowColor.copy(alpha = envelope * GLOW_CENTER_ALPHA),
                    0.5f to glowColor.copy(alpha = envelope * GLOW_MID_ALPHA),
                    1.0f to Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                radius = size.width * GLOW_RADIUS_FACTOR,
            ),
            size = size,
        )

        // 2. Diagonal shimmer sweep — travels from -30% to +130% of the box width
        val sweepX = -size.width * 0.3f + shimmerFraction * size.width * 1.6f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    shimmerColor.copy(alpha = envelope * SHIMMER_SHOULDER_ALPHA),
                    shimmerColor.copy(alpha = envelope * SHIMMER_PEAK_ALPHA),
                    shimmerColor.copy(alpha = envelope * SHIMMER_SHOULDER_ALPHA),
                    Color.Transparent,
                ),
                start = Offset(sweepX - SHIMMER_HALF_WIDTH_PX, 0f),
                end = Offset(sweepX + SHIMMER_HALF_WIDTH_PX, size.height),
            ),
            size = size,
        )

        // 3. Bright border
        drawRect(
            color = glowColor.copy(alpha = envelope * BORDER_ALPHA),
            topLeft = Offset.Zero,
            size = size,
            style = Stroke(width = BORDER_STROKE_DP.dp.toPx()),
        )
    }
}

// ── Tuning constants — adjust these to change the look without touching logic ────

/** Alpha at the dead center of the radial glow. */
private const val GLOW_CENTER_ALPHA = 0.45f

/** Alpha at the midpoint radius of the radial glow. */
private const val GLOW_MID_ALPHA = 0.20f

/** Radial gradient radius as a fraction of box width (> 1 means it bleeds to edges). */
private const val GLOW_RADIUS_FACTOR = 1.1f

/** Peak alpha of the shimmer band center. */
private const val SHIMMER_PEAK_ALPHA = 0.60f

/** Alpha on the soft shoulder edges of the shimmer band. */
private const val SHIMMER_SHOULDER_ALPHA = 0.14f

/** Half-width of the shimmer band in pixels (full band = 2× this). */
private const val SHIMMER_HALF_WIDTH_PX = 65f

/** Alpha of the glowing border ring. */
private const val BORDER_ALPHA = 0.90f

/** Thickness of the border ring in dp. */
private const val BORDER_STROKE_DP = 1.5f

/** Progress threshold below which the envelope is still fading in. */
private const val ENVELOPE_FADE_IN = 0.08f

/** Progress threshold above which the envelope starts fading out. */
private const val ENVELOPE_FADE_OUT = 0.80f
