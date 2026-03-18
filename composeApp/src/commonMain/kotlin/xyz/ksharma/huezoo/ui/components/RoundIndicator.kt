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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors

/**
 * A row of dots showing game round progress.
 *
 * - Inactive rounds: [HuezooColors.TextDisabled] dot
 * - Active round: [HuezooColors.AccentCyan] dot with pulse animation
 * - Completed rounds: [HuezooColors.AccentGreen] dot
 *
 * [currentRound] is 1-based (1 = first round active).
 */
@Composable
fun RoundIndicator(
    totalRounds: Int,
    currentRound: Int,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    spacing: Dp = 8.dp,
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
                    isCompleted -> HuezooColors.AccentGreen
                    isActive -> HuezooColors.AccentCyan
                    else -> HuezooColors.TextDisabled
                },
                animationSpec = tween(300),
                label = "dotColor_$index",
            )

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .then(if (isActive) Modifier.scale(pulseScale) else Modifier)
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
            RoundIndicator(totalRounds = 6, currentRound = 1)
            RoundIndicator(totalRounds = 6, currentRound = 3)
            RoundIndicator(totalRounds = 6, currentRound = 6)
            RoundIndicator(totalRounds = 6, currentRound = 7)
        }
    }
}
