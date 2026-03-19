package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors

private val CardShape = RoundedCornerShape(20.dp)
private val BadgeShape = RoundedCornerShape(6.dp)
private val CardShadowOffset: Dp = 6.dp

/**
 * Neo-brutalist game card for the Home screen.
 *
 * - Hard [identityColor] shadow at [CardShadowOffset] offset (no blur).
 * - On press the card sinks INTO the shadow and springs back on release.
 * - [visualContent] slot at the top — pass a Canvas illustration or colored swatch area
 *   to give the card a strong visual identity. Defaults to a simple colored band.
 * - [badgeText] shown top-right (e.g. "5 tries left", "Done").
 * - [triesText] shown below the subtitle for remaining-try highlights.
 * - [personalBest] shown in muted text at the bottom.
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
    visualContent: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1f else 0f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 80)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "cardPress",
    )

    val shadowOffsetPx = with(LocalDensity.current) { CardShadowOffset.toPx() }
    val shadowColor = identityColor.copy(alpha = if (enabled) 1f else 0.3f)

    // Outer box reserves space for the shadow
    Box(
        modifier = modifier
            .widthIn(min = 280.dp)
            .padding(end = CardShadowOffset, bottom = CardShadowOffset),
    ) {
        // Hard shadow layer — stays fixed
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = CardShadowOffset, y = CardShadowOffset)
                .background(shadowColor, CardShape),
        )

        // Card layer — sinks into shadow on press
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = pressProgress * shadowOffsetPx
                    translationY = pressProgress * shadowOffsetPx
                }
                .border(2.dp, identityColor.copy(alpha = if (enabled) 0.6f else 0.2f), CardShape)
                .background(HuezooColors.SurfaceL2, CardShape)
                .clip(CardShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
        ) {
            Column {
                // ── Visual area ────────────────────────────────────────────────
                if (visualContent != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(identityColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        visualContent()
                    }
                } else {
                    // Default: identity color band with a subtle pattern hint
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(identityColor),
                    )
                }

                // ── Content ────────────────────────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    // Header row: title + badge (wraps at large font scale)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = HuezooColors.TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f),
                            )
                            if (badgeText != null) {
                                Box(
                                    modifier = Modifier
                                        .background(identityColor, BadgeShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HuezooColors.Background,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

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
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    if (personalBest != null) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = personalBest,
                            style = MaterialTheme.typography.labelSmall,
                            color = HuezooColors.TextDisabled,
                        )
                    }
                }
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun GameCardPreview() {
    HuezooPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            GameCard(
                title = "The Threshold",
                subtitle = "How sharp are your eyes?",
                identityColor = HuezooColors.GameThreshold,
                onClick = {},
                badgeText = "3 tries",
                personalBest = "Best: \u0394E 1.2",
            )
            GameCard(
                title = "Daily Challenge",
                subtitle = "Same puzzle for everyone today",
                identityColor = HuezooColors.GameDaily,
                onClick = {},
                badgeText = "Done",
                personalBest = "Score: 840",
            )
            GameCard(
                title = "Color Memory",
                subtitle = "Remember the sequence",
                identityColor = HuezooColors.GameMemory,
                onClick = {},
                triesText = "2 tries remaining",
            )
        }
    }
}
