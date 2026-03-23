package xyz.ksharma.huezoo.domain.color

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate

/**
 * Game color API — the single interface all ViewModels and game logic depend on.
 *
 * Defined as an interface so that:
 * - ViewModels receive it via Koin injection and never depend on a concrete class.
 * - Unit tests can pass a [FakeColorEngine] or use [DefaultColorEngine] with a seeded
 *   [kotlin.random.Random] for fully deterministic, reproducible test runs.
 * - Future implementations (e.g. one that avoids deuteranopia-unfriendly hues) can
 *   be swapped in without changing any call site.
 *
 * **Rule:** no ViewModel, Screen, or game-logic class may import [DefaultColorEngine]
 * directly. All colour work goes through this interface.
 *
 * @see DefaultColorEngine — production implementation, registered in [colorModule].
 */
interface ColorEngine {

    /**
     * Generates a random vivid swatch color suitable for display in the game.
     *
     * "Vivid" means saturation 65–100% and lightness 30–70% — never grey,
     * never near-black, never near-white.
     */
    fun randomVividColor(): Color

    /**
     * Like [randomVividColor] but guarantees the generated hue is outside the band
     * `[excludeHue - excludeHueWidth, excludeHue + excludeHueWidth]` (modulo 360°).
     *
     * Used so the game never shows a base color that visually matches the player's
     * current level accent — keeping the game palette clearly distinct from the UI chrome.
     *
     * @param excludeHue      Centre of the forbidden hue band (0–360°).
     * @param excludeHueWidth Half-width of the forbidden band in degrees (default 40°).
     */
    fun randomVividColorExcluding(excludeHue: Float, excludeHueWidth: Float = 40f): Color

    /**
     * Generates a color that is perceptually [targetDeltaE] ΔE away from [base].
     *
     * This is the core mechanic: [base] is shown on two swatches, and the returned
     * color is shown on the third. The player must tap the odd one out.
     *
     * @param base        The reference color (shown on 2 of the 3 swatches).
     * @param targetDeltaE The desired CIEDE2000 distance. Smaller = harder.
     * @return A color at approximately [targetDeltaE] from [base].
     */
    fun generateOddSwatch(base: Color, targetDeltaE: Float): Color

    /**
     * Returns a deterministic vivid color for [date].
     *
     * Every player on the same calendar day gets the same base color, making
     * the Daily Challenge fair and score-comparable. The mapping is a pure
     * function of (year, month, day) — no server call required.
     */
    fun seededColorForDate(date: LocalDate): Color
}
