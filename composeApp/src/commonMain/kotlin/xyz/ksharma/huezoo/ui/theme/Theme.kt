package xyz.ksharma.huezoo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dark color scheme (default — game is primarily dark) ──────────────────────
private val huezooDarkColorScheme = darkColorScheme(
    primary = HuezooColors.AccentCyan,
    background = HuezooColors.Background,
    surface = HuezooColors.SurfaceL1,
    surfaceVariant = HuezooColors.SurfaceL2,
    onPrimary = HuezooColors.Background,
    onBackground = HuezooColors.TextPrimary,       // white
    onSurface = HuezooColors.TextPrimary,
    onSurfaceVariant = HuezooColors.TextSecondary, // muted purple-grey
    error = HuezooColors.AccentMagenta,
    secondaryContainer = HuezooColors.SurfaceL3,
    onSecondaryContainer = HuezooColors.TextPrimary,
)

// ── Light color scheme — game still feels vibrant on white ────────────────────
private val huezooLightColorScheme = lightColorScheme(
    primary = Color(0xFF006E8F),           // deeper cyan readable on white
    background = Color(0xFFF6F6FF),        // off-white with a hint of purple
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEDF8),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0D0D1A),      // near-black for text
    onSurface = Color(0xFF0D0D1A),
    onSurfaceVariant = Color(0xFF55556A),  // secondary text on light bg
    error = Color(0xFFB5004A),
    secondaryContainer = Color(0xFFDFDFF0),
    onSecondaryContainer = Color(0xFF0D0D1A),
)

@Composable
fun HuezooTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) huezooDarkColorScheme else huezooLightColorScheme,
        typography = huezooTypography(),
        content = content,
    )
}
