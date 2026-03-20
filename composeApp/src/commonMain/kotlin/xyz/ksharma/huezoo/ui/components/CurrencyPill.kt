package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.ParallelogramBack
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val GemIconSize = 18.dp
private val GemShadowOffset = 3.dp
private const val GEM_SHADOW_ALPHA = 0.35f

/**
 * Parallelogram gem counter shown in the top bar on every screen.
 *
 * Matches the back-button style: AccentCyan shadow offset (+3, +3), SurfaceL3 fill,
 * ParallelogramBack shape. Amount rolls in with a slot-machine slide-up animation
 * whenever the value changes. A brief scale pulse accompanies the roll.
 *
 * Pass `painterResource(Res.drawable.ic_gem)` as [icon].
 */
@Composable
fun CurrencyPill(
    amount: Int,
    icon: Painter,
    modifier: Modifier = Modifier,
) {
    // Scale pulse on value change (1.0 → 1.15 → 1.0, spring)
    val pulseScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "gemPulse",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                clip = false
            }
            .shapedShadow(
                shape = ParallelogramBack,
                color = HuezooColors.AccentCyan.copy(alpha = GEM_SHADOW_ALPHA),
                offsetX = GemShadowOffset,
                offsetY = GemShadowOffset,
            )
            .clip(ParallelogramBack)
            .background(HuezooColors.SurfaceL3)
            .padding(horizontal = 14.dp, vertical = HuezooSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(GemIconSize),
            )
            Spacer(Modifier.width(6.dp))
            // Slot-machine roll: new value slides in from bottom, old slides out to top
            AnimatedContent(
                targetState = amount,
                transitionSpec = {
                    (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(180))) togetherWith
                        (slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(120)))
                },
                label = "gemCount",
            ) { displayAmount ->
                HuezooLabelLarge(
                    text = formatGems(displayAmount),
                    color = HuezooColors.AccentCyan,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

private fun formatGems(amount: Int): String = when {
    amount >= 1_000_000 -> "${amount / 1_000_000}M GEMS"
    amount >= 1_000 -> "${amount / 1_000},${(amount % 1_000).toString().padStart(3, '0')} GEMS"
    else -> "$amount GEMS"
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun CurrencyPillPreview() {
    val gemPlaceholder = androidx.compose.ui.graphics.vector.rememberVectorPainter(
        image = androidx.compose.ui.graphics.vector.ImageVector.Builder(
            defaultWidth = 18.dp,
            defaultHeight = 18.dp,
            viewportWidth = 18f,
            viewportHeight = 18f,
        ).addPath(
            pathData = listOf(
                androidx.compose.ui.graphics.vector.PathNode.MoveTo(9f, 1f),
                androidx.compose.ui.graphics.vector.PathNode.LineTo(3f, 7f),
                androidx.compose.ui.graphics.vector.PathNode.LineTo(9f, 17f),
                androidx.compose.ui.graphics.vector.PathNode.LineTo(15f, 7f),
                androidx.compose.ui.graphics.vector.PathNode.Close,
            ),
            fill = androidx.compose.ui.graphics.SolidColor(HuezooColors.AccentCyan),
        ).build(),
    )
    HuezooPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
            CurrencyPill(amount = 512, icon = gemPlaceholder)
            CurrencyPill(amount = 1250, icon = gemPlaceholder)
        }
    }
}
