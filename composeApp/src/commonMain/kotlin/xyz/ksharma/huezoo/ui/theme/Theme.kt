package xyz.ksharma.huezoo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val huezooColorScheme = darkColorScheme(
    primary = HuezooColors.AccentCyan,
    background = HuezooColors.Background,
    surface = HuezooColors.SurfaceL1,
    surfaceVariant = HuezooColors.SurfaceL2,
    onPrimary = HuezooColors.Background,
    onBackground = HuezooColors.TextPrimary,
    onSurface = HuezooColors.TextPrimary,
    onSurfaceVariant = HuezooColors.TextSecondary,
    error = HuezooColors.AccentMagenta,
    secondaryContainer = HuezooColors.SurfaceL3,
    onSecondaryContainer = HuezooColors.TextPrimary,
)

@Composable
fun HuezooTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = huezooColorScheme,
        typography = huezooTypography(),
        content = content,
    )
}
