package xyz.ksharma.huezoo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ── Dark color scheme (default — game is primarily dark) ──────────────────────
//
// Surface tier → Material3 slot mapping:
//   SurfaceL0 (void)    → surfaceContainerLowest
//   SurfaceL1 (low)     → surface / surfaceContainerLow
//   SurfaceL2 (mid)     → surfaceContainer
//   SurfaceL3 (high)    → surfaceContainerHigh
//   SurfaceL4 (highest) → surfaceContainerHighest / surfaceVariant
//
private val huezooDarkColorScheme = darkColorScheme(
    primary = HuezooColors.AccentCyan,
    background = HuezooColors.Background,
    surface = HuezooColors.SurfaceL1,
    surfaceVariant = HuezooColors.SurfaceL4,
    surfaceContainer = HuezooColors.SurfaceL2,
    surfaceContainerLow = HuezooColors.SurfaceL1,
    surfaceContainerHigh = HuezooColors.SurfaceL3,
    surfaceContainerHighest = HuezooColors.SurfaceL4,
    surfaceContainerLowest = HuezooColors.SurfaceL0,
    onPrimary = HuezooColors.Background,
    onBackground = HuezooColors.TextPrimary,
    onSurface = HuezooColors.TextPrimary,
    onSurfaceVariant = HuezooColors.TextSecondary,
    error = HuezooColors.AccentMagenta,
    secondaryContainer = HuezooColors.SurfaceL3,
    onSecondaryContainer = HuezooColors.TextPrimary,
)

@Composable
fun HuezooTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = huezooDarkColorScheme,
        typography = huezooTypography(),
        content = content,
    )
}
