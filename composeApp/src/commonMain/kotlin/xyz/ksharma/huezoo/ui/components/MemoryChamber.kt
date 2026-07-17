package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.onColor
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val ShelfOffset = 4.dp
private val LedSize = 8.dp
private const val LED_PULSE_MS = 700
private const val LED_PULSE_MIN_ALPHA = 0.35f
private val StripeSpacing = 14.dp
private const val WAITING_STRIPE_ALPHA = 0.12f
private const val QUESTION_MARK_ALPHA = 0.55f
private const val QUESTION_MARK_FONT_SP = 96
private const val ENTRANCE_MS_UP = 280
private const val ENTRANCE_MS_SETTLE = 140
private const val ENTRANCE_SCALE_FROM = 0.85f
private const val ENTRANCE_SCALE_OVERSHOOT = 1.05f
private val EntranceEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
private const val UNSEALED_PILL_BG_ALPHA = 0.45f

/**
 * One chamber of the Twin Lock layout for Color Memory Match.
 *
 * Renders a neo-brutalist slab (4×4 dp shelf under it) whose face is either the
 * live color, a striped waiting pattern with a "?" glyph, or the color behind an
 * animated [ChamberShutter]. The top tag row shows the chamber label and an LED
 * with its state (LIVE / SEAL / WAIT / OPEN).
 *
 * @param color The chamber's color — shown when [state] is Live, Sealed (behind
 *   the shutter), or Revealed.
 * @param accent Identity accent for the rim, LED, and waiting pattern.
 * @param entranceKey Changing this value replays the color entrance pop
 *   (`scale 0.85 → 1.05 → 1.0`).
 * @param showUnsealedPill Chamber A only — bottom "UNSEALED" pill during the reveal.
 */
@Composable
fun MemoryChamber(
    label: String,
    state: MemoryChamberState,
    color: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    entranceKey: Int = 0,
    showUnsealedPill: Boolean = false,
) {
    val colorVisible = state != MemoryChamberState.Waiting

    // Entrance pop when the chamber goes Live (or reveals) for a new round
    val entranceScale = remember { Animatable(1f) }
    LaunchedEffect(entranceKey, state == MemoryChamberState.Live) {
        if (state == MemoryChamberState.Live) {
            entranceScale.snapTo(ENTRANCE_SCALE_FROM)
            entranceScale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = ENTRANCE_MS_UP + ENTRANCE_MS_SETTLE
                    ENTRANCE_SCALE_OVERSHOOT at ENTRANCE_MS_UP using EntranceEasing
                },
            )
        }
    }

    val faceColor by animateColorAsState(
        targetValue = if (colorVisible) color else HuezooColors.SurfaceL1,
        animationSpec = tween(200),
        label = "chamberFace",
    )

    Box(
        modifier = modifier
            .shapedShadow(RectangleShape, HuezooColors.SurfaceL4, ShelfOffset, ShelfOffset)
            .graphicsLayer {
                scaleX = entranceScale.value
                scaleY = entranceScale.value
            }
            .background(faceColor)
            .border(HuezooSize.BorderThin, accent.copy(alpha = 0.5f)),
    ) {
        // Waiting: diagonal stripe pattern + "?" glyph
        if (state == MemoryChamberState.Waiting) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val spacing = StripeSpacing.toPx()
                val stripeColor = accent.copy(alpha = WAITING_STRIPE_ALPHA)
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = stripeColor,
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = spacing / 2f,
                    )
                    x += spacing
                }
            }
            androidx.compose.material3.Text(
                text = "?",
                style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(
                    fontSize = QUESTION_MARK_FONT_SP.sp,
                    lineHeight = QUESTION_MARK_FONT_SP.sp,
                ),
                color = accent.copy(alpha = QUESTION_MARK_ALPHA),
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Shutter — covers the face while Sealed, slides away on Reveal
        ChamberShutter(visible = state == MemoryChamberState.Sealed)

        // Tag row — label + LED + state word
        val onFace = if (colorVisible && state != MemoryChamberState.Sealed) {
            faceColor.onColor
        } else {
            HuezooColors.TextSecondary
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(HuezooSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HuezooLabelSmall(
                text = label,
                color = onFace,
                fontWeight = FontWeight.ExtraBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChamberLed(state = state, accent = accent)
                Spacer(Modifier.width(HuezooSpacing.xs))
                HuezooLabelSmall(
                    text = when (state) {
                        MemoryChamberState.Live -> "LIVE"
                        MemoryChamberState.Sealed -> "SEAL"
                        MemoryChamberState.Waiting -> "WAIT"
                        MemoryChamberState.Revealed -> "OPEN"
                    },
                    color = onFace,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        // "UNSEALED" answer-key pill — bottom-centered on reveal
        if (showUnsealedPill && state == MemoryChamberState.Revealed) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = HuezooSpacing.sm)
                    .background(
                        Color.Black.copy(alpha = UNSEALED_PILL_BG_ALPHA),
                        RoundedCornerShape(HuezooSize.CornerSmall),
                    )
                    .padding(horizontal = HuezooSpacing.sm, vertical = 2.dp),
            ) {
                HuezooLabelSmall(
                    text = "UNSEALED",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun ChamberLed(
    state: MemoryChamberState,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberInfiniteTransition(label = "ledPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = LED_PULSE_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(LED_PULSE_MS), RepeatMode.Reverse),
        label = "ledPulseAlpha",
    )
    val ledColor = when (state) {
        MemoryChamberState.Live -> accent.copy(alpha = pulseAlpha)
        MemoryChamberState.Sealed -> HuezooColors.AccentYellow
        MemoryChamberState.Waiting -> HuezooColors.TextDisabled
        MemoryChamberState.Revealed -> HuezooColors.AccentGreen
    }
    Box(modifier = modifier.size(LedSize).background(ledColor, CircleShape))
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun MemoryChamberStatesPreview() {
    HuezooPreviewTheme {
        Row(
            modifier = Modifier.padding(HuezooSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            MemoryChamber(
                label = "CHAMBER A",
                state = MemoryChamberState.Live,
                color = HuezooColors.AccentPurple,
                accent = HuezooColors.AccentCyan,
                modifier = Modifier.weight(1f).size(160.dp),
            )
            MemoryChamber(
                label = "CHAMBER B",
                state = MemoryChamberState.Waiting,
                color = HuezooColors.AccentPurple,
                accent = HuezooColors.AccentMagenta,
                modifier = Modifier.weight(1f).size(160.dp),
            )
        }
    }
}
