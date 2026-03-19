package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.PillShape
import xyz.ksharma.huezoo.ui.theme.SquircleLarge
import xyz.ksharma.huezoo.ui.theme.darken

private const val DECIMAL_SCALE = 10
private const val ENTRANCE_OFFSET_DP = 60f
private const val ENTRANCE_SCALE_START = 0.9f
private const val ENTRANCE_DAMPING = 0.7f
private const val ENTRANCE_STIFFNESS = 200f
private const val COUNTUP_STIFFNESS = 80f

private val ResultShelf = 8.dp
private val FrameInset = 5.dp

/**
 * Share-ready result card shown at end of a game.
 *
 * Layout: outer frame (identityColor) + inner panel (Background) with a
 * thick bottom shelf — same candy-style layered approach as GameCard.
 *
 * Animations (baked in):
 * - Slide up 60dp + scale 0.9 → 1.0 spring entrance
 * - Count-up for [score] and [deltaE] via spring
 *
 * [identityColor] fills the outer frame and tints the top accent band.
 * [percentileText] e.g. "Better than 94% of players" — pass null to hide.
 */
@Composable
fun ResultCard(
    gameTitle: String,
    deltaE: Float,
    score: Int,
    roundsSurvived: Int,
    identityColor: Color,
    modifier: Modifier = Modifier,
    percentileText: String? = null,
) {
    // ── Entrance animation ────────────────────────────────────────────────────
    val offsetY = remember { Animatable(ENTRANCE_OFFSET_DP) }
    val cardScale = remember { Animatable(ENTRANCE_SCALE_START) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = ENTRANCE_DAMPING, stiffness = ENTRANCE_STIFFNESS),
            )
        }
        launch {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = ENTRANCE_STIFFNESS),
            )
        }
        launch { alpha.animateTo(1f, tween(300)) }
    }

    // ── Count-up ──────────────────────────────────────────────────────────────
    val displayScore = remember { Animatable(0f) }
    val displayDeltaE = remember { Animatable(0f) }

    LaunchedEffect(score, deltaE) {
        launch {
            displayScore.animateTo(
                score.toFloat(),
                spring(stiffness = COUNTUP_STIFFNESS, dampingRatio = Spring.DampingRatioNoBouncy),
            )
        }
        launch {
            displayDeltaE.animateTo(
                deltaE,
                spring(stiffness = COUNTUP_STIFFNESS, dampingRatio = Spring.DampingRatioNoBouncy),
            )
        }
    }

    val scoreInt = displayScore.value.toInt()
    val deInt = displayDeltaE.value.toInt()
    val deDec = ((displayDeltaE.value - deInt.toFloat()) * DECIMAL_SCALE).toInt()
    val formattedDeltaE = "$deInt.$deDec"

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = offsetY.value
                scaleX = cardScale.value
                scaleY = cardScale.value
                this.alpha = alpha.value
            }
            .fillMaxWidth()
            .padding(bottom = ResultShelf),
    ) {
        // Shelf
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 0.dp, y = ResultShelf)
                .background(identityColor.darken(0.5f), SquircleLarge),
        )

        // Outer frame (identityColor)
        Box(
            modifier = Modifier
                .background(identityColor, SquircleLarge)
                .clip(SquircleLarge),
        ) {
            // Inner dark panel
            Box(
                modifier = Modifier
                    .padding(FrameInset)
                    .background(HuezooColors.Background, SquircleLarge)
                    .clip(SquircleLarge),
            ) {
                Column {
                    // ── Identity accent band ──────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HuezooSpacing.sm)
                            .background(identityColor),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(HuezooSpacing.lg),
                    ) {
                        // ── Game label ────────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .background(identityColor.copy(alpha = 0.15f), PillShape)
                                .padding(horizontal = HuezooSpacing.md, vertical = 4.dp),
                        ) {
                            HuezooLabelSmall(
                                text = gameTitle.uppercase(),
                                color = identityColor,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            )
                        }

                        Spacer(Modifier.height(HuezooSpacing.lg))

                        // ── ΔE hero number ────────────────────────────────────
                        HuezooLabelSmall(text = "COLOR DELTA")
                        HuezooDisplayLarge(text = formattedDeltaE)

                        Spacer(Modifier.height(HuezooSpacing.lg))

                        // ── Divider ───────────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(HuezooColors.SurfaceL3),
                        )

                        Spacer(Modifier.height(HuezooSpacing.md))

                        // ── Stats row ─────────────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            ResultStatColumn(label = "SCORE", value = "$scoreInt")
                            ResultStatColumn(
                                label = "ROUNDS",
                                value = "$roundsSurvived",
                                alignment = Alignment.End,
                            )
                        }

                        // ── Percentile ────────────────────────────────────────
                        if (percentileText != null) {
                            Spacer(Modifier.height(HuezooSpacing.md))
                            HuezooLabelMedium(text = percentileText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        HuezooLabelSmall(text = label)
        HuezooDisplayMedium(text = value)
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun ResultCardThresholdPreview() {
    HuezooPreviewTheme {
        ResultCard(
            gameTitle = "The Threshold",
            deltaE = 1.2f,
            score = 840,
            roundsSurvived = 12,
            identityColor = HuezooColors.GameThreshold,
            percentileText = "Better than 94% of players",
        )
    }
}

@PreviewComponent
@Composable
private fun ResultCardDailyPreview() {
    HuezooPreviewTheme {
        ResultCard(
            gameTitle = "Daily Challenge",
            deltaE = 2.4f,
            score = 510,
            roundsSurvived = 4,
            identityColor = HuezooColors.GameDaily,
        )
    }
}
