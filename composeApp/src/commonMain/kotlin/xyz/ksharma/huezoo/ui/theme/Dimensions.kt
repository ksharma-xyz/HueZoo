package xyz.ksharma.huezoo.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing tokens — use for padding, gaps, and margins between elements.
 *
 * ```
 * xs   4 dp  — icon-to-label, tight badge padding
 * sm   8 dp  — small gaps, dot spacing
 * md  16 dp  — standard content padding
 * lg  24 dp  — card content padding, large gaps
 * xl  32 dp  — section separators
 * xxl 48 dp  — hero / screen-level breathing room
 * ```
 */
object HuezooSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/**
 * Size tokens — named component dimensions. Use these instead of hardcoded dp values so the
 * full system can be rescaled in one place.
 *
 * Categories:
 * - **Shadow** — hard offset for the neo-brutalist press effect
 * - **Border** — consistent stroke widths
 * - **Icon** — standard icon sizes
 * - **Component** — per-component named sizes
 */
object HuezooSize {

    // ── Neo-brutalist shadow offsets ───────────────────────────────────────────
    /** Hard shadow offset for buttons. Card uses [ShadowCard]. */
    val ShadowButton = 4.dp

    /** Hard shadow offset for game cards. Slightly deeper than [ShadowButton]. */
    val ShadowCard = 6.dp

    // ── Border widths ─────────────────────────────────────────────────────────
    /** Ghost button / badge outline. */
    val BorderThin = 1.5.dp

    /** Card border / emphasis border. */
    val BorderMedium = 2.dp

    // ── Icon sizes ─────────────────────────────────────────────────────────────
    val IconSmall = 16.dp
    val IconMedium = 24.dp
    val IconLarge = 32.dp

    // ── Component sizes ────────────────────────────────────────────────────────
    /** Default diameter for each dot in [RoundIndicator]. */
    val DotIndicator = 8.dp

    /** GameCard visual area height (top identity slot). */
    val CardVisualArea = 100.dp

    /** Inner horizontal padding for pill badges. */
    val BadgeHorizontalPad = 14.dp

    /** Inner vertical padding for pill badges. */
    val BadgeVerticalPad = 4.dp

    // ── Corner radii ──────────────────────────────────────────────────────────
    /** Badge pill corners, small chips. */
    val CornerSmall = 6.dp

    /** Button corners — chunky game-feel. */
    val CornerButton = 12.dp

    /** Card corners. */
    val CornerCard = 20.dp

    /** Bottom sheet top corners. */
    val CornerSheet = 32.dp
}
