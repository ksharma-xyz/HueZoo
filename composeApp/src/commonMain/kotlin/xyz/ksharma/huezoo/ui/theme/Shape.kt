@file:Suppress("MatchingDeclarationName") // File intentionally contains SquircleShape + preset vals

package xyz.ksharma.huezoo.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Superellipse (squircle) shape: |x/a|^n + |y/b|^n = 1.
 * [exponent] = 4 → classic iOS-style squircle. Higher = closer to rectangle.
 */
class SquircleShape(private val exponent: Float = 4f) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(squirclePath(size, exponent))

    private fun squirclePath(size: Size, n: Float): Path {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val path = Path()
        val steps = SQUIRCLE_PATH_STEPS
        for (i in 0..steps) {
            val angle = 2.0 * PI * i / steps
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            val x = cx + cx * cosA.sign() * abs(cosA).toDouble().pow(2.0 / n).toFloat()
            val y = cy + cy * sinA.sign() * abs(sinA).toDouble().pow(2.0 / n).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun Float.sign(): Float = if (this >= 0f) 1f else -1f
}

private const val SQUIRCLE_PATH_STEPS = 180

// ── Presets ──────────────────────────────────────────────────────────────────

/** Rounder — small chips, badges */
val SquircleSmall = SquircleShape(exponent = 3.5f)

/** Standard iOS-style squircle — default */
val SquircleMedium = SquircleShape(exponent = 4f)

/** Slightly more square — large surfaces */
val SquircleLarge = SquircleShape(exponent = 5f)

/** Game cards */
val SquircleCard = SquircleShape(exponent = 4f)

/** Buttons */
val SquircleButton = SquircleShape(exponent = 4f)

/** Full pill — text buttons, price CTAs, currency pills */
val PillShape = androidx.compose.foundation.shape.RoundedCornerShape(50)

/**
 * Parallelogram (italic rectangle) shape.
 * [skewFraction] is the horizontal offset applied to the top edge as a fraction of the component
 * height — 0.25f gives a ~14° lean, matching the italic feel of Bebas Neue headings.
 *
 * Used by:
 * - [TopBarBackButton] (back navigation in HuezooTopBar)
 * - DS.7 SkewedStatChip (time/score readouts on gameplay screens)
 *
 * Layout for RTL/LTR: always leans right (top-right over bottom-left).
 */
class ParallelogramShape(private val skewFraction: Float = 0.25f) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(parallelogramPath(size))

    private fun parallelogramPath(size: Size): Path {
        val skew = size.height * skewFraction
        return Path().apply {
            moveTo(skew, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - skew, size.height)
            lineTo(0f, size.height)
            close()
        }
    }
}

/** Back button / kinetic chip — 25% height skew */
val ParallelogramBack = ParallelogramShape(skewFraction = 0.25f)
