package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.SquircleCard

/**
 * Tappable card representing a game mode on the Home screen.
 *
 * - [identityColor] paints a 4-dp accent bar at the top and a subtle gradient tint.
 * - [badgeText] appears top-right (e.g. "5 tries left", "Done ✓").
 * - [triesText] shown below the subtitle — highlights low attempt count.
 * - [personalBest] shown bottom-left in muted text.
 *
 * Press animation: scale 1.0 → 0.97 instantly, spring back (DS.5 baked in).
 */
@Composable
fun GameCard(
    title: String,
    subtitle: String,
    identityColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    triesText: String? = null,
    personalBest: String? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 80)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "cardScale",
    )

    Box(
        modifier = modifier
            .widthIn(min = 280.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(SquircleCard)
            .background(HuezooColors.SurfaceL2, SquircleCard)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        // Subtle identity-color gradient tint
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to identityColor.copy(alpha = 0.10f),
                            0.6f to Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    ),
                ),
        )

        Column(modifier = Modifier.padding(20.dp)) {
            // ── Header row ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Identity accent dot
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(identityColor, SquircleCard),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = HuezooColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (badgeText != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(identityColor.copy(alpha = 0.2f), SquircleCard)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = identityColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Subtitle ──────────────────────────────────────────────────────
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = HuezooColors.TextSecondary,
            )

            if (triesText != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = triesText,
                    style = MaterialTheme.typography.labelSmall,
                    color = identityColor,
                )
            }

            // ── Personal best ─────────────────────────────────────────────────
            if (personalBest != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = personalBest,
                    style = MaterialTheme.typography.labelSmall,
                    color = HuezooColors.TextDisabled,
                )
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun GameCardPreview() {
    HuezooPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GameCard(
                title = "The Threshold",
                subtitle = "How sharp are your eyes?",
                identityColor = HuezooColors.GameThreshold,
                onClick = {},
                badgeText = "3 tries left",
                personalBest = "Personal best: ΔE 1.2",
            )
            GameCard(
                title = "Daily Challenge",
                subtitle = "Today's puzzle — same for everyone",
                identityColor = HuezooColors.GameDaily,
                onClick = {},
                badgeText = "Done ✓",
                personalBest = "Score: 840",
            )
        }
    }
}
