package xyz.ksharma.huezoo.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

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
 * Draws a hard directional shadow that exactly follows [shape], offset to the bottom-right.
 *
 * Unlike [depthShadow] (which is a soft multi-layer blur approximation), this draws a single
 * opaque/translucent copy of the composable's own shape at (+[offsetX], +[offsetY]) — the
 * neo-brutalist press-depth effect. Shadow is visible only on the right and bottom edges
 * because it is fully covered by the composable face on the top and left.
 *
 * Works with ANY [Shape]: [ParallelogramShape], [SquircleShape], `RoundedCornerShape`, `PillShape`.
 * The shadow is drawn via [drawBehind] so it is NOT clipped by a [Modifier.clip] applied after
 * this modifier — the shadow correctly overhangs the composable bounds.
 *
 * Consistent usage rules (keep these everywhere in the DS):
 * - [offsetX] / [offsetY] = **4 dp** for interactive buttons and chips
 * - [color] = `AccentCyan.copy(alpha = 0.30f)` for cyan-themed components
 * - Always pair with a matching [Modifier.clip] + [Modifier.background] on the same composable
 *   so the face shape matches the shadow shape
 *
 * Used by: [TopBarBackButton], DS.7 SkewedStatChip (and any future shaped button)
 */
fun Modifier.shapedShadow(
    shape: Shape,
    color: Color,
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp,
): Modifier = drawBehind {
    val ox = offsetX.toPx()
    val oy = offsetY.toPx()
    translate(left = ox, top = oy) {
        when (val outline = shape.createOutline(size, layoutDirection, this@drawBehind)) {
            is Outline.Generic -> drawPath(outline.path, color = color)
            is Outline.Rectangle -> drawRect(
                color = color,
                topLeft = outline.rect.topLeft,
                size = outline.rect.size,
            )
            is Outline.Rounded -> drawRoundRect(
                color = color,
                cornerRadius = outline.roundRect.topLeftCornerRadius,
            )
        }
    }
}

// ── Organic cloud glow ────────────────────────────────────────────────────────

/**
 * Internal blob descriptor for [organicGlow].
 *
 * @param nx          Normalised X position relative to the composable width  (0 = left, 1 = right; values outside [0,1] place the blob outside the bounds).
 * @param ny          Normalised Y position relative to the composable height.
 * @param baseRadiusDp Base radius of the blob in dp (breathe animation scales this ±25 %).
 * @param color       Colour of this blob — varies per position to get chromatic variety.
 * @param phaseOffset Offset into the shared phase oscillation so each blob breathes at a different moment.
 * @param maxAlpha    Peak opacity when the blob is fully "inhaled".
 */
private data class GlowBlob(
    val nx: Float,
    val ny: Float,
    val baseRadiusDp: Float,
    val color: Color,
    val phaseOffset: Float,
    val maxAlpha: Float,
)

/**
 * Organic, cloud-like neon glow that bleeds unevenly around the composable.
 *
 * Ten animated blobs of varying size, colour, and breathing phase are scattered
 * around the card edges.  Their overlap creates a living halo that is thick in
 * some places and thin in others — nothing like a uniform ring.
 *
 * Three colour variants are auto-derived from [color]:
 * - **base** — the colour as-is
 * - **rose** — warmer, more pink (red channel kept, green +0.20, blue ×0.40)
 * - **violet** — cooler, deeper purple (red ×0.40, blue +0.15)
 *
 * Each blob is drawn as four concentric filled circles from outer-faint to
 * inner-bright, accumulating into a smooth radial falloff without platform blur.
 *
 * NOTE: the parent must not clip so the blobs extending beyond the composable
 * bounds stay visible.
 */
@Composable
fun Modifier.organicGlow(
    color: Color,
): Modifier {
    // ── Derived colour variants ──────────────────────────────────────────────
    val colorRose = remember(color) {
        Color(
            red = color.red,
            green = (color.green + 0.20f).coerceIn(0f, 1f),
            blue = (color.blue * 0.40f).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }
    val colorViolet = remember(color) {
        Color(
            red = (color.red * 0.40f).coerceIn(0f, 1f),
            green = 0f,
            blue = (color.blue + 0.15f).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }

    // ── Blob layout ─────────────────────────────────────────────────────────
    // Positions are chosen so corners get colour-accent spikes and edges get
    // large soft patches — intentionally asymmetric.
    val blobs = remember(color, colorRose, colorViolet) {
        listOf(
            // Top edge — big magenta patch left-of-center
            GlowBlob(
                nx = 0.25f,
                ny = -0.10f,
                baseRadiusDp = 36f,
                color = color,
                phaseOffset = 0.00f,
                maxAlpha = 0.55f,
            ),
            // Top edge — smaller rose blob, right-of-center
            GlowBlob(
                nx = 0.72f,
                ny = -0.07f,
                baseRadiusDp = 22f,
                color = colorRose,
                phaseOffset = (PI * 0.60f).toFloat(),
                maxAlpha = 0.42f,
            ),
            // Top-right corner — violet spike
            GlowBlob(
                nx = 0.97f,
                ny = -0.04f,
                baseRadiusDp = 16f,
                color = colorViolet,
                phaseOffset = (PI * 1.10f).toFloat(),
                maxAlpha = 0.35f,
            ),
            // Right edge — magenta, upper half
            GlowBlob(
                nx = 1.09f,
                ny = 0.38f,
                baseRadiusDp = 26f,
                color = color,
                phaseOffset = (PI * 0.40f).toFloat(),
                maxAlpha = 0.50f,
            ),
            // Right edge — violet, lower half (thinner → gap feel)
            GlowBlob(
                nx = 1.06f,
                ny = 0.74f,
                baseRadiusDp = 16f,
                color = colorViolet,
                phaseOffset = (PI * 1.60f).toFloat(),
                maxAlpha = 0.36f,
            ),
            // Bottom edge — large rose cloud, right-of-center
            GlowBlob(
                nx = 0.62f,
                ny = 1.10f,
                baseRadiusDp = 40f,
                color = colorRose,
                phaseOffset = (PI * 1.30f).toFloat(),
                maxAlpha = 0.55f,
            ),
            // Bottom edge — magenta, left-of-center (smaller → asymmetry)
            GlowBlob(
                nx = 0.18f,
                ny = 1.07f,
                baseRadiusDp = 20f,
                color = color,
                phaseOffset = (PI * 0.90f).toFloat(),
                maxAlpha = 0.40f,
            ),
            // Bottom-left corner — rose accent
            GlowBlob(
                nx = 0.04f,
                ny = 1.05f,
                baseRadiusDp = 14f,
                color = colorRose,
                phaseOffset = (PI * 1.50f).toFloat(),
                maxAlpha = 0.30f,
            ),
            // Left edge — big violet cloud (creates deep purple flank)
            GlowBlob(
                nx = -0.10f,
                ny = 0.65f,
                baseRadiusDp = 30f,
                color = colorViolet,
                phaseOffset = (PI * 1.90f).toFloat(),
                maxAlpha = 0.50f,
            ),
            // Left edge — small rose, upper (breaks symmetry with right)
            GlowBlob(
                nx = -0.06f,
                ny = 0.20f,
                baseRadiusDp = 18f,
                color = colorRose,
                phaseOffset = (PI * 0.25f).toFloat(),
                maxAlpha = 0.36f,
            ),
        )
    }

    // ── Shared animation phase (one value drives all blobs via their offset) ─
    val transition = rememberInfiniteTransition(label = "organicGlow")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glowPhase",
    )

    // ── Draw ─────────────────────────────────────────────────────────────────
    return this.drawBehind {
        val w = size.width
        val h = size.height

        blobs.forEach { blob ->
            // Breathe: 0 → 1 sine wave unique to this blob
            val breathe = sin(phase + blob.phaseOffset) * 0.5f + 0.5f
            val radius = blob.baseRadiusDp.dp.toPx() * (0.75f + breathe * 0.50f)
            val peakAlpha = blob.maxAlpha * (0.40f + breathe * 0.60f)
            val cx = w * blob.nx
            val cy = h * blob.ny

            // Four concentric circles — outermost large+faint, innermost small+bright.
            // Source-Over accumulation makes the center progressively more saturated.
            drawCircle(
                color = blob.color.copy(alpha = peakAlpha * 0.10f),
                radius = radius,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = blob.color.copy(alpha = peakAlpha * 0.18f),
                radius = radius * 0.62f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = blob.color.copy(alpha = peakAlpha * 0.26f),
                radius = radius * 0.35f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = blob.color.copy(alpha = peakAlpha * 0.20f),
                radius = radius * 0.14f,
                center = Offset(cx, cy),
            )
        }
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
