package xyz.ksharma.huezoo.ui.theme

import androidx.compose.ui.graphics.Color

object HuezooColors {
    // Backgrounds
    val Background = Color(0xFF080810)
    val SurfaceL1 = Color(0xFF12121E)
    val SurfaceL2 = Color(0xFF1C1C2E)
    val SurfaceL3 = Color(0xFF26263A)

    // Accents
    val AccentCyan = Color(0xFF00E5FF)
    val AccentMagenta = Color(0xFFFF2D78)
    val AccentYellow = Color(0xFFFFE600)
    val AccentPurple = Color(0xFF9B5DE5)
    val AccentGreen = Color(0xFF00F5A0)

    // Text
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8888AA)
    val TextDisabled = Color(0xFF44445A)

    // Glows (40% alpha of accent)
    val GlowCyan = Color(0x4000E5FF)
    val GlowMagenta = Color(0x40FF2D78)
    val GlowYellow = Color(0x40FFE600)
    val GlowPurple = Color(0x409B5DE5)
    val GlowGreen = Color(0x4000F5A0)

    // Game identity colors — richer, less neon for dark UI
    val GameThreshold = Color(0xFF7B6FF0) // Soft indigo-violet
    val GameDaily = Color(0xFFFF8A50) // Warm coral-orange
    val GameMemory = Color(0xFF00C9A7) // Teal
}
