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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.SquircleCard
import xyz.ksharma.huezoo.ui.theme.colorGlow

/**
 * Share-ready 1:1 result card shown at end of a game.
 *
 * Animations (baked in):
 * - Slide up 60 dp + scale 0.9 → 1.0 entrance on composition (DS.5.7)
 * - Count-up for [score] and [deltaE] via spring (DS.5.3)
 *
 * [identityColor] paints the radial gradient and glow border.
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
    val offsetY = remember { Animatable(60f) }
    val cardScale = remember { Animatable(0.9f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
            )
        }
        launch {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 200f),
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
                spring(stiffness = 80f, dampingRatio = Spring.DampingRatioNoBouncy),
            )
        }
        launch { displayDeltaE.animateTo(deltaE, spring(stiffness = 80f, dampingRatio = Spring.DampingRatioNoBouncy)) }
    }

    val scoreInt = displayScore.value.toInt()

    val deInt = displayDeltaE.value.toInt()
    val deDec = ((displayDeltaE.value - deInt.toFloat()) * 10).toInt()
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
            .aspectRatio(1f)
            .colorGlow(color = identityColor, glowRadius = 20.dp, cornerRadius = 24.dp)
            .clip(SquircleCard)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        identityColor.copy(alpha = 0.25f),
                        HuezooColors.SurfaceL2,
                        HuezooColors.SurfaceL1,
                    ),
                    radius = 600f,
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text = gameTitle,
                style = MaterialTheme.typography.labelMedium,
                color = identityColor,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(16.dp))

            // ── ΔE big number ────────────────────────────────────────────────
            Column {
                Text(
                    text = "ΔE",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HuezooColors.TextSecondary,
                )
                Text(
                    text = formattedDeltaE,
                    style = MaterialTheme.typography.displayLarge,
                    color = HuezooColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Stats row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = HuezooColors.TextSecondary,
                    )
                    Text(
                        text = "$scoreInt",
                        style = MaterialTheme.typography.headlineMedium,
                        color = HuezooColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rounds",
                        style = MaterialTheme.typography.labelSmall,
                        color = HuezooColors.TextSecondary,
                    )
                    Text(
                        text = "$roundsSurvived",
                        style = MaterialTheme.typography.headlineMedium,
                        color = HuezooColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ── Percentile ───────────────────────────────────────────────────
            if (percentileText != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = percentileText,
                    style = MaterialTheme.typography.labelMedium,
                    color = HuezooColors.TextSecondary,
                )
            }
        }
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
