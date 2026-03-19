package xyz.ksharma.huezoo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.ParallelogramBack
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val ChipPaddingH = 14.dp
private val ChipPaddingV = 6.dp
private const val CHIP_SHADOW_ALPHA = 0.30f

/**
 * Kinetic stat readout chip — parallelogram shape matching the top-bar back button.
 *
 * Displays a small [label] above a large [value], styled with the game's identity accent.
 * The italic parallelogram shape signals "live data in motion" — distinct from passive pill badges.
 *
 * Shadow rule: same [shapedShadow] pattern as all DS buttons — 4dp offset, accent at 30% alpha.
 *
 * Usage (gameplay HUD):
 * ```kotlin
 * Row(horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
 *     SkewedStatChip(label = "ROUND",   value = "3",    accentColor = GameThreshold)
 *     SkewedStatChip(label = "ΔE",      value = "2.4",  accentColor = AccentCyan)
 *     SkewedStatChip(label = "TRIES",   value = "4",    accentColor = AccentMagenta)
 * }
 * ```
 *
 * @param label Small all-caps descriptor (e.g. "ROUND", "SCORE", "ΔE", "TRIES")
 * @param value Large Bebas Neue number or short string
 * @param accentColor Chip shadow + value text color — use the screen's identity color
 */
@Composable
fun SkewedStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = HuezooColors.AccentCyan,
) {
    Box(
        modifier = modifier
            .shapedShadow(
                shape = ParallelogramBack,
                color = accentColor.copy(alpha = CHIP_SHADOW_ALPHA),
            )
            .clip(ParallelogramBack)
            .background(HuezooColors.SurfaceL2)
            .padding(horizontal = ChipPaddingH, vertical = ChipPaddingV),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HuezooLabelSmall(
                text = label,
                color = HuezooColors.TextSecondary,
            )
            HuezooDisplaySmall(
                text = value,
                color = accentColor,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun SkewedStatChipPreview() {
    HuezooPreviewTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                HuezooSpacing.sm,
            ),
        ) {
            SkewedStatChip(label = "ROUND", value = "3", accentColor = HuezooColors.GameThreshold)
            SkewedStatChip(label = "ΔE", value = "2.4", accentColor = HuezooColors.AccentCyan)
            SkewedStatChip(label = "TRIES", value = "4", accentColor = HuezooColors.AccentMagenta)
        }
    }
}
