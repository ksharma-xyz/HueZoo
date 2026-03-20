package xyz.ksharma.huezoo.ui.model

/**
 * Controls the physical size of the swatch tiles in [RadialSwatchLayout].
 *
 * - [Normal] — default 1.0× tile dimensions. Harder: smaller targets, subtler differences.
 * - [Medium] — 1.2× tile dimensions. Easier: larger surface area to detect ΔE.
 *
 * The active size is set via [xyz.ksharma.huezoo.ui.components.ACTIVE_SWATCH_SIZE] in
 * RadialSwatchLayout.kt — one constant to change for debugging. Users always see one size.
 */
enum class SwatchSize {
    Normal,
    Medium,
}
