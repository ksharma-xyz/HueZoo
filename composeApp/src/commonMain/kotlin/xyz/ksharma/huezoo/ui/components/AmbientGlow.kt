package xyz.ksharma.huezoo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors

private const val GLOW_ALPHA = 0.10f
private const val GLOW_RADIUS_FRACTION = 0.70f

/**
 * Screen-level ambient background glow — two large radial gradients anchored at opposite corners.
 *
 * Wraps screen content in a [Box]. The two glows are drawn via [drawBehind] so they never
 * intercept touch events and impose zero overhead on layout measurement.
 *
 * ```
 * ╔═══════════════════════╗
 * ║ ◉ primaryColor        ║  ← top-start corner glow
 * ║          content…     ║
 * ║       primaryColor ◉  ║  ← bottom-end corner glow (secondaryColor)
 * ╚═══════════════════════╝
 * ```
 *
 * Usage — pass game identity colors per screen:
 * ```kotlin
 * // Home screen
 * AmbientGlowBackground { HomeContent() }
 *
 * // Threshold game screen
 * AmbientGlowBackground(
 *     primaryColor = HuezooColors.GameThreshold,
 *     secondaryColor = HuezooColors.AccentCyan,
 * ) { ThresholdContent() }
 * ```
 *
 * Default colors are cyan (top-start) + magenta (bottom-end) — the app's primary accent pair.
 */
@Composable
fun AmbientGlowBackground(
    modifier: Modifier = Modifier,
    primaryColor: Color = HuezooColors.AccentCyan,
    secondaryColor: Color = HuezooColors.AccentMagenta,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HuezooColors.Background)
            .drawBehind {
                val radius = size.minDimension * GLOW_RADIUS_FRACTION

                // Top-start glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = GLOW_ALPHA),
                            Color.Transparent,
                        ),
                        center = Offset(0f, 0f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(0f, 0f),
                )

                // Bottom-end glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = GLOW_ALPHA),
                            Color.Transparent,
                        ),
                        center = Offset(size.width, size.height),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(size.width, size.height),
                )
            },
        content = content,
    )
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun AmbientGlowDefaultPreview() {
    HuezooPreviewTheme {
        AmbientGlowBackground {}
    }
}

@PreviewComponent
@Composable
private fun AmbientGlowThresholdPreview() {
    HuezooPreviewTheme {
        AmbientGlowBackground(
            primaryColor = HuezooColors.GameThreshold,
            secondaryColor = HuezooColors.AccentPurple,
        ) {}
    }
}
