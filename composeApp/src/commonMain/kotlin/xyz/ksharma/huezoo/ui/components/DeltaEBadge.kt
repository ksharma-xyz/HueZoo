package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.SquircleSmall

/**
 * Badge showing the current ΔE (color difference) value.
 *
 * Color encodes difficulty:
 * - ΔE > 3  → Cyan   (easy)
 * - 1 ≤ ΔE ≤ 3 → Yellow (medium)
 * - ΔE < 1  → Magenta (hard)
 *
 * Animations (baked in):
 * - Spring-scale + fade appear on composition (DS.5)
 * - Count-up from 0 to [deltaE] via spring
 * - Color crossfade as difficulty changes
 */
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
            deltaE > 3f -> HuezooColors.AccentCyan
            deltaE > 1f -> HuezooColors.AccentYellow
            else -> HuezooColors.AccentMagenta
        },
        animationSpec = tween(400),
        label = "badgeColor",
    )

    val intPart = displayValue.value.toInt()
    val decPart = ((displayValue.value - intPart.toFloat()) * 10).toInt()
    val formatted = "$intPart.$decPart"

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = badgeScale.value
                scaleY = badgeScale.value
                alpha = badgeAlpha.value
            }
            .background(badgeColor.copy(alpha = 0.15f), SquircleSmall)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            DeltaEBadge(deltaE = 4.2f)
            DeltaEBadge(deltaE = 2.1f)
            DeltaEBadge(deltaE = 0.8f)
        }
    }
}
