package xyz.ksharma.huezoo.ui.theme

import androidx.compose.ui.graphics.Color

object HuezooColors {
    // Backgrounds — 5-tier stack from void to highest surface
    val Background = Color(0xFF0D0D16) // App canvas — neon reads as emitted light
    val SurfaceL0 = Color(0xFF000000) // Deepest wells — track bg, recessed slots
    val SurfaceL1 = Color(0xFF13121C) // Low surface — section backgrounds
    val SurfaceL2 = Color(0xFF191923) // Mid surface — card bodies, sheet interior
    val SurfaceL3 = Color(0xFF1F1F2A) // High surface — active cards, interactive slots
    val SurfaceL4 = Color(0xFF252531) // Highest surface — focused/selected state, popovers

    // Accents
    val AccentCyan = Color(0xFF00E5FF)
    val AccentMagenta = Color(0xFFFF2D78)
    val AccentYellow = Color(0xFFFFE600)
    val AccentPurple = Color(0xFF9B5DE5)
    val AccentGreen = Color(0xFF00F5A0)

    // Text
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9898BB) // ~5.5:1 on SurfaceL1
    val TextDisabled = Color(0xFF777799) // ~3.5:1 on SurfaceL1

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

    // ── Shelf colors (bottom-only shadow — darker version of each accent) ──────
    val ShelfCyan = Color(0xFF009DB3)
    val ShelfMagenta = Color(0xFFB3004E)
    val ShelfYellow = Color(0xFFB8A000)
    val ShelfGreen = Color(0xFF00A86A)
    val ShelfPurple = Color(0xFF6B3DAD)
    val ShelfMaster = Color(0xFFBB8800) // darker amber shelf for Master level

    // ── Game-action button colors ─────────────────────────────────────────────
    val ActionConfirm = Color(0xFF22C55E) // green — confirm / correct
    val ShelfConfirm = Color(0xFF15803D)
    val ActionDismiss = Color(0xFFEF4444) // red — dismiss / wrong
    val ShelfDismiss = Color(0xFFB91C1C)
    val ActionTry = Color(0xFF3B82F6) // blue — secondary / try
    val ShelfTry = Color(0xFF1D4ED8)

    // ── Purchase CTA ──────────────────────────────────────────────────────────
    val PriceGreen = Color(0xFF4ADE80) // bright green for price button
    val ShelfPrice = Color(0xFF16A34A)

    // ── Reward tile (Brawl Stars style) ──────────────────────────────────────
    val TileBorder = Color(0xFFE2E8F0) // near-white border on tiles
    val TileSurface = Color(0xFF1E3A5F) // deep blue tile background
    val TileShelf = Color(0xFF0F2040) // darker blue shelf below tile

    // ── Currency ──────────────────────────────────────────────────────────────
    val GemGreen = Color(0xFF00E676) // gem/currency icon tint
}

/** Returns a darkened version of this color by multiplying RGB channels by [factor]. */
fun Color.darken(factor: Float = 0.6f): Color = Color(
    red = red * factor,
    green = green * factor,
    blue = blue * factor,
    alpha = alpha,
)
