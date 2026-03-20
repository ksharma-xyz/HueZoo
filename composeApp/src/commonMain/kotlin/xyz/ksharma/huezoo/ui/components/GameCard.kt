package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.PillShape
import xyz.ksharma.huezoo.ui.theme.darken
import xyz.ksharma.huezoo.ui.theme.onColor
import xyz.ksharma.huezoo.ui.theme.rimLight
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val CardShape = RoundedCornerShape(20.dp)
private val CardShelf = 8.dp
private val FrameInset = 5.dp
private val IllustrationSize = 72.dp
private val IllustrationAreaHeight = 90.dp
private val HeroIllustrationAreaHeight = 140.dp

/**
 * Candy-style game card for the Home screen.
 *
 * Layout:
 * - Outer frame filled with [identityColor] (CardShape)
 * - Inner panel (SurfaceL1) inset by [FrameInset] on all sides
 * - Illustration area at top (90dp): [badgeText] overlaid top-right
 * - Content area: title, subtitle, [triesText], [personalBest]
 * - Hard [identityColor] shelf (8dp, bottom only) — face presses down on tap
 *
 * Pass [illustrationPainter] to show a game illustration, or [visualContent] for
 * a fully custom composable in the illustration slot.
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
    /** Live countdown string shown below tries/personal best (e.g. "Resets in 2h 14m"). */
    countdownText: String? = null,
    enabled: Boolean = true,
    /** Hero mode: taller illustration area — use for the primary/featured game card. */
    isHero: Boolean = false,
    illustrationPainter: Painter? = null,
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

    val shelfPx = with(LocalDensity.current) { CardShelf.toPx() }
    val frameColor = if (enabled) identityColor else identityColor.copy(alpha = 0.4f)
    val shelfColor = (if (enabled) identityColor else identityColor.copy(alpha = 0.4f)).darken(0.55f)

    Box(
        modifier = modifier
            .widthIn(min = 280.dp)
            .padding(bottom = CardShelf),
    ) {
        // Card face — shapedShadow draws the shelf; face slides into it on press
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = pressProgress * shelfPx
                    clip = false // let the shadow render outside the layer bounds
                }
                .shapedShadow(
                    shape = CardShape,
                    color = shelfColor,
                    offsetX = 0.dp,
                    offsetY = CardShelf,
                )
                .background(frameColor, CardShape)
                .clip(CardShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
        ) {
            // Inner panel inset from the identity-color frame, with rim light for depth
            Box(
                modifier = Modifier
                    .padding(FrameInset)
                    .background(HuezooColors.SurfaceL2, CardShape)
                    .rimLight(cornerRadius = 20.dp)
                    .clip(CardShape),
            ) {
                Column {
                    // ── Illustration area ───────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isHero) HeroIllustrationAreaHeight else IllustrationAreaHeight)
                            .background(identityColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            visualContent != null -> visualContent()
                            illustrationPainter != null -> Image(
                                painter = illustrationPainter,
                                contentDescription = title,
                                modifier = Modifier.size(IllustrationSize),
                            )
                        }

                        // Badge overlaid top-right of illustration area
                        if (badgeText != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(HuezooSpacing.sm),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(identityColor, PillShape)
                                        .padding(
                                            horizontal = HuezooSize.BadgeHorizontalPad,
                                            vertical = HuezooSize.BadgeVerticalPad,
                                        ),
                                ) {
                                    HuezooLabelSmall(
                                        text = badgeText,
                                        color = identityColor.onColor, // WCAG-safe on any identity color
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                            }
                        }
                    }

                    // ── Text content ────────────────────────────────────────
                    Column(
                        modifier = Modifier.padding(
                            horizontal = HuezooSpacing.md,
                            vertical = HuezooSpacing.sm + 4.dp,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FlowRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
                                verticalArrangement = Arrangement.spacedBy(HuezooSpacing.xs),
                                itemVerticalAlignment = Alignment.CenterVertically,
                            ) {
                                HuezooTitleMedium(
                                    text = title,
                                    modifier = Modifier.weight(1f),
                                    // SurfaceL2 is always dark — use static token, not theme-based
                                    color = HuezooColors.TextPrimary,
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        HuezooBodyMedium(
                            text = subtitle,
                            color = HuezooColors.TextSecondary,
                        )

                        if (triesText != null) {
                            Spacer(Modifier.height(HuezooSpacing.xs))
                            HuezooLabelSmall(
                                text = triesText,
                                color = identityColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        if (personalBest != null) {
                            Spacer(Modifier.height(HuezooSpacing.sm))
                            HuezooLabelSmall(
                                text = personalBest,
                                color = HuezooColors.TextDisabled,
                            )
                        }

                        if (countdownText != null) {
                            Spacer(Modifier.height(HuezooSpacing.xs))
                            HuezooLabelSmall(
                                text = countdownText,
                                color = HuezooColors.TextDisabled,
                            )
                        }
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
