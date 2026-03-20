package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.ParallelogramBack
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val GemShadowOffset = 3.dp
private const val GEM_SHADOW_ALPHA = 0.35f

/**
 * Parallelogram gem counter shown in the top bar on every screen.
 *
 * Layout: large [amount] number + small "GEMS" label side-by-side.
 * No icon — number is the hero. The amount rolls in with a slot-machine
 * slide-up AnimatedContent transition whenever the value changes.
 *
 * Shape matches the back button: ParallelogramBack + AccentCyan shadow.
 */
@Composable
fun CurrencyPill(
    amount: Int,
    modifier: Modifier = Modifier,
) {
    // Scale pulse when gems increase (skip first composition)
    val pulseScale = remember { Animatable(1f) }
    val prevAmount = remember { mutableIntStateOf(amount) }
    LaunchedEffect(amount) {
        if (amount != prevAmount.intValue) {
            prevAmount.intValue = amount
            pulseScale.snapTo(1f)
            pulseScale.animateTo(
                1.2f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 600f),
            )
            pulseScale.animateTo(1f, spring(stiffness = 400f))
        }
    }

    val accent = LocalPlayerAccentColor.current
    Box(
        modifier = modifier
            .graphicsLayer {
                clip = false
                scaleX = pulseScale.value
                scaleY = pulseScale.value
            }
            .shapedShadow(
                shape = ParallelogramBack,
                color = accent.copy(alpha = GEM_SHADOW_ALPHA),
                offsetX = GemShadowOffset,
                offsetY = GemShadowOffset,
            )
            .clip(ParallelogramBack)
            .background(HuezooColors.SurfaceL3)
            .padding(horizontal = 14.dp, vertical = HuezooSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Number — big, slot-machine roll on change
            AnimatedContent(
                targetState = amount,
                transitionSpec = {
                    (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(180))) togetherWith
                        (slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(120)))
                },
                label = "gemCount",
            ) { displayAmount ->
                HuezooHeadlineMedium(
                    text = formatNumber(displayAmount),
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            // Label — small, static
            HuezooLabelSmall(
                text = "GEMS",
                color = accent.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 2.sp.value.dp),
            )
        }
    }
}

private fun formatNumber(amount: Int): String = when {
    amount >= 1_000_000 -> "${amount / 1_000_000}M"
    amount >= 1_000 -> "${amount / 1_000},${(amount % 1_000).toString().padStart(3, '0')}"
    else -> amount.toString()
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun CurrencyPillPreview() {
    HuezooPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
            CurrencyPill(amount = 512)
            CurrencyPill(amount = 1250)
        }
    }
}
