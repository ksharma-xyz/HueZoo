package xyz.ksharma.huezoo.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ── WCAG 2.1 contrast thresholds ─────────────────────────────────────────────
// https://www.w3.org/TR/WCAG21/#contrast-minimum

/** Minimum contrast ratio for normal text (< 18pt regular / < 14pt bold). WCAG AA. */
const val CONTRAST_AA_NORMAL = 4.5f

/** Minimum contrast ratio for large text (≥ 18pt regular or ≥ 14pt bold). WCAG AA. */
const val CONTRAST_AA_LARGE = 3.0f

// ── Foreground fallback candidates ───────────────────────────────────────────

/** Near-black foreground used on bright/light backgrounds. */
private val TEXT_ON_LIGHT = Color(0xFF0D0D1A)

/** White foreground used on dark backgrounds. */
private val TEXT_ON_DARK = Color(0xFFFFFFFF)

// ── Extensions ────────────────────────────────────────────────────────────────

/**
 * Returns the WCAG 2.1 contrast ratio between this color and [other].
 *
 * Formula: (L1 + 0.05) / (L2 + 0.05), where L1 is the lighter relative luminance.
 * - Ratio ≥ 4.5  → passes WCAG AA for normal text (< 18sp regular, < 14sp bold)
 * - Ratio ≥ 3.0  → passes WCAG AA for large text  (≥ 18sp regular or ≥ 14sp bold)
 * - Ratio = 21   → maximum (black on white)
 * - Ratio = 1    → minimum (same color on same color)
 *
 * ```kotlin
 * val ratio = HuezooColors.AccentCyan.contrastRatio(HuezooColors.Background) // ~12.9
 * ```
 */
fun Color.contrastRatio(other: Color): Float {
    val l1 = this.luminance() + 0.05f
    val l2 = other.luminance() + 0.05f
    return if (l1 > l2) l1 / l2 else l2 / l1
}

/**
 * Returns the most readable foreground (text / icon tint) color for this background.
 *
 * Resolution order:
 * 1. [preferred] — if provided AND passes WCAG AA (4.5:1), use it
 * 2. [TEXT_ON_LIGHT] (`#0D0D1A`) — near-black, preferred for bright backgrounds
 * 3. [TEXT_ON_DARK]  (`#FFFFFF`) — white, preferred for dark backgrounds
 * 4. Pure `Color.Black` / `Color.White` — last-resort high-contrast fallback
 *
 * Example:
 * ```kotlin
 * val textColor = HuezooColors.AccentCyan.foregroundColor()            // → #0D0D1A (dark)
 * val textColor = HuezooColors.AccentMagenta.foregroundColor()         // → #0D0D1A (dark)
 * val textColor = HuezooColors.Background.foregroundColor()            // → #FFFFFF (white)
 * val textColor = HuezooColors.AccentCyan.foregroundColor(Color.White) // White? No (1.5 ratio) → #0D0D1A
 * val textColor = HuezooColors.Background.foregroundColor(Color.White) // White passes → #FFFFFF
 * ```
 */
fun Color.foregroundColor(preferred: Color? = null): Color {
    preferred?.let {
        if (it.contrastRatio(this) >= CONTRAST_AA_NORMAL) return it
    }
    return when {
        TEXT_ON_LIGHT.contrastRatio(this) >= CONTRAST_AA_NORMAL -> TEXT_ON_LIGHT
        TEXT_ON_DARK.contrastRatio(this) >= CONTRAST_AA_NORMAL -> TEXT_ON_DARK
        Color.Black.contrastRatio(this) >= CONTRAST_AA_NORMAL -> Color.Black
        else -> Color.White
    }
}

/**
 * Shorthand for [foregroundColor] with no preferred color.
 *
 * Returns a WCAG AA-compliant text or icon tint color for use ON TOP OF this color as a background.
 *
 * ```kotlin
 * // In a button component:
 * val textColor = faceColor.onColor
 *
 * // In a badge:
 * HuezooLabelSmall(text = badgeText, color = identityColor.onColor)
 *
 * // In a custom chip:
 * val iconTint = chipColor.onColor
 * ```
 *
 * NEVER hardcode `Color.White` or `Color.Black` for text on a colored background — use this instead.
 */
val Color.onColor: Color get() = foregroundColor()
