package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.colorGlow

/**
 * A row of dots showing game round progress.
 *
 * - Inactive: subtle [HuezooColors.SurfaceL3] dot
 * - Active: [activeColor] dot with pulse animation
 * - Completed: [completedColor] dot (dimmed)
 *
 * [currentRound] is 1-based (1 = first round active).
 * Pass the game's identity color as [activeColor] to match the game theme.
 */
@Composable
fun RoundIndicator(
    totalRounds: Int,
    currentRound: Int,
    modifier: Modifier = Modifier,
    dotSize: Dp = HuezooSize.DotIndicator,
    spacing: Dp = HuezooSpacing.sm,
    activeColor: Color = HuezooColors.AccentCyan,
    completedColor: Color = HuezooColors.AccentGreen,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "roundPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "activeDotPulse",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalRounds) { index ->
            val roundNumber = index + 1
            val isActive = roundNumber == currentRound
            val isCompleted = roundNumber < currentRound

            val dotColor by animateColorAsState(
                targetValue = when {
                    isCompleted -> completedColor.copy(alpha = 0.6f)
                    isActive -> activeColor
                    else -> HuezooColors.SurfaceL3
                },
                animationSpec = tween(300),
                label = "dotColor_$index",
            )

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .then(if (isActive) Modifier.scale(pulseScale) else Modifier)
                    .then(
                        if (isActive) {
                            Modifier.colorGlow(
                                color = activeColor,
                                glowRadius = 8.dp,
                                cornerRadius = dotSize / 2,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .background(dotColor, CircleShape),
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun RoundIndicatorPreview() {
    HuezooPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RoundIndicator(
                totalRounds = 6,
                currentRound = 3,
                activeColor = HuezooColors.GameThreshold,
                completedColor = HuezooColors.GameThreshold,
            )
            RoundIndicator(
                totalRounds = 6,
                currentRound = 5,
                activeColor = HuezooColors.GameDaily,
                completedColor = HuezooColors.GameDaily,
            )
            // All completed
            RoundIndicator(
                totalRounds = 6,
                currentRound = 7,
                activeColor = HuezooColors.GameMemory,
                completedColor = HuezooColors.GameMemory,
            )
        }
    }
}
