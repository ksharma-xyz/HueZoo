package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.SquircleSmall

private const val DELTA_E_EASY_THRESHOLD = 3f
private const val DELTA_E_MEDIUM_THRESHOLD = 1f
private const val DECIMAL_SCALE = 10

/**
 * Badge showing the current ΔE (color difference) value.
 *
 * Color encodes difficulty:
 * - ΔE > 3  → Cyan   (easy — large difference, easy to spot)
 * - 1 ≤ ΔE ≤ 3 → Yellow (medium)
 * - ΔE < 1  → Magenta (hard — tiny difference)
 *
 * Animations (baked in):
 * - Spring scale + fade appear on composition
 * - Count-up from 0 to [deltaE] via spring
 * - Color crossfade as difficulty changes
 */
// typography and background and shadows can be better
@Composable
fun DeltaEBadge(
    deltaE: Float,
    modifier: Modifier = Modifier,
    label: String = "ΔE",
) {
    // ── Appear animation ──────────────────────────────────────────────────────
    val badgeScale = remember { Animatable(0.5f) }
    val badgeAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        launch { badgeAlpha.animateTo(1f, tween(200)) }
    }

    // ── Count-up ──────────────────────────────────────────────────────────────
    val displayValue = remember { Animatable(0f) }
    LaunchedEffect(deltaE) {
        displayValue.animateTo(
            targetValue = deltaE,
            animationSpec = spring(stiffness = 80f, dampingRatio = Spring.DampingRatioNoBouncy),
        )
    }

    // ── Color by difficulty ───────────────────────────────────────────────────
    val badgeColor by animateColorAsState(
        targetValue = when {
            deltaE > DELTA_E_EASY_THRESHOLD -> HuezooColors.AccentCyan
            deltaE > DELTA_E_MEDIUM_THRESHOLD -> HuezooColors.AccentYellow
            else -> HuezooColors.AccentMagenta
        },
        animationSpec = tween(400),
        label = "badgeColor",
    )

    val intPart = displayValue.value.toInt()
    val decPart = ((displayValue.value - intPart.toFloat()) * DECIMAL_SCALE).toInt()
    val formatted = "$intPart.$decPart"

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = badgeScale.value
                scaleY = badgeScale.value
                alpha = badgeAlpha.value
            }
            .background(badgeColor.copy(alpha = 0.15f), SquircleSmall)
            .padding(horizontal = HuezooSize.BadgeHorizontalPad, vertical = HuezooSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatted,
                style = MaterialTheme.typography.titleMedium,
                color = badgeColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor.copy(alpha = 0.8f),
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun DeltaEBadgePreview() {
    HuezooPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
            DeltaEBadge(deltaE = 4.2f)
            DeltaEBadge(deltaE = 2.1f)
            DeltaEBadge(deltaE = 0.8f)
        }
    }
}
