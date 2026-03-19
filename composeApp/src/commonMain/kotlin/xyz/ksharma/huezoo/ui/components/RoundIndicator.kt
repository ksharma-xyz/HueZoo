package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
                    isCompleted -> completedColor.copy(alpha = 0.7f)
                    isActive -> activeColor
                    else -> HuezooColors.SurfaceL3
                },
                animationSpec = tween(300),
                label = "dotColor_$index",
            )

            // Active dot is slightly larger; completed/inactive stay at base size
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.2f else 1f,
                animationSpec = tween(250),
                label = "dotScale_$index",
            )

            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(dotSize)
                    .then(
                        // Active: solid white ring border — crisp, no blur
                        if (isActive) {
                            Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
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
