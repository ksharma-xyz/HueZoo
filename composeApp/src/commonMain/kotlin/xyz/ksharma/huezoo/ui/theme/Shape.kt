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
 * Flat-top regular hexagon — used for SwatchBlock tiles.
 *
 * Vertices at 0°, 60°, 120°, 180°, 240°, 300° from center.
 * The horizontal radius equals half the composable width, so the hexagon
 * always spans the full width of its bounding box; height is ~86.6% of width.
 */
class HexagonShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(hexagonPath(size))

    private fun hexagonPath(size: Size): Path {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rx = size.width / 2f
        val path = Path()
        for (i in 0..5) {
            val angle = PI / 3.0 * i
            val px = (cx + rx * cos(angle)).toFloat()
            val py = (cy + rx * sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return path
    }
}

/** Flat-top regular hexagon — default swatch tile shape */
val HexagonSwatch = HexagonShape()

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

/**
 * Petal shape — a symmetric teardrop / leaf with the pointed tip at the TOP of the bounding box
 * and the widest, rounded part at the BOTTOM.
 *
 * Designed for the flower swatch layout:
 * - Place each petal so its top-center (tip) sits at the flower's centre point.
 * - Rotate each instance by 0°, 60°, 120°, 180°, 240°, 300° with
 *   `transformOrigin = TransformOrigin(0.5f, 0f)` to fan the petals outward.
 * - Animate `scaleY` 0 → 1 (same pivot) to "grow" the petal outward from the tip on unfold,
 *   and 1 → 0 to "retract" it back to the tip on fold.
 *
 * The shape is drawn with two cubic bezier curves: one for the left edge and one for the right.
 */
class PetalShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(petalPath(size))

    private fun petalPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        return Path().apply {
            // Start at the tip (top-centre)
            moveTo(cx, 0f)
            // Right edge: from tip → sweeps out to the right → curves around the bottom
            cubicTo(
                w * 0.95f,
                h * 0.15f, // control 1 — right side, near top
                w,
                h * 0.60f, // control 2 — far right, mid-height
                cx,
                h, // end — bottom centre (the round belly)
            )
            // Left edge: from bottom centre → sweeps back up the left side → returns to tip
            cubicTo(
                0f,
                h * 0.60f, // control 1 — far left, mid-height
                w * 0.05f,
                h * 0.15f, // control 2 — left side, near top
                cx,
                0f, // end — back to tip
            )
            close()
        }
    }
}

/** Default petal tile for the flower swatch layout. */
val SwatchPetal = PetalShape()


/**
 * Diamond shape — a perfect rhombus whose **top tip sits at (width/2, 0)** and bottom tip at
 * (width/2, height), with the widest points at the vertical midpoint.
 *
 * Compatible with [RadialSwatchLayout]: placing each tile with its top tip at [centerGap]
 * distance from the layout centre and rotating radially produces a ring of inward-pointing
 * diamonds.
 */
class DiamondShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(diamondPath(size))

    private fun diamondPath(size: Size): Path {
        val cx = size.width / 2f
        val cy = size.height / 2f
        return Path().apply {
            moveTo(cx, 0f) // top tip
            lineTo(size.width, cy) // right point
            lineTo(cx, size.height) // bottom tip
            lineTo(0f, cy) // left point
            close()
        }
    }
}

/** Default diamond tile for the DiamondHalo layout. */
val DiamondSwatch = DiamondShape()

/**
 * Kite-diamond (asymmetric rhombus) shape — four **straight** edges with sharp points at all
 * four corners, but intentionally **not** symmetric:
 *
 * - **Inner tip** (top, faces layout centre): narrower angle (~60 °) — more pointed, gem-like.
 * - **Outer tip** (bottom, faces outward): wider angle (~76 °) — same as the old symmetric
 *   diamond, so the outer silhouette looks unchanged.
 * - Widest point sits at 58 % of the tile height (below centre), producing the elongated
 *   inner-triangle proportions seen in [docs/ideas/swatch_diamong.png].
 *
 * Tip at (cx, 0) — radial pivot math identical to [PetalShape] and [DiamondShape].
 */
class RoundedShieldShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(shieldPath(size))

    private fun shieldPath(size: Size): Path {
        val cx = size.width / 2f
        // Widest point below mid-height → inner portion is taller (more pointed inner tip)
        // while outer portion stays at roughly the same angle as a symmetric diamond.
        val sidePtY = size.height * 0.58f
        return Path().apply {
            moveTo(cx, 0f)              // inner tip — top centre, points toward layout centre
            lineTo(size.width, sidePtY) // right point — widest edge (58 % down)
            lineTo(cx, size.height)     // outer tip — bottom centre, points away from centre
            lineTo(0f, sidePtY)         // left point — widest edge
            close()
        }
    }
}

/** Rounded shield/badge tile — wide at the outer edge, tapers to an inner tip. */
val ShieldSwatch = RoundedShieldShape()

/**
 * Citrus-slice (crescent) shape — a landscape tile with a **concave inner arc** facing the
 * flower centre and a **convex outer arc** facing outward.  The two tips sit at the left and
 * right edges at mid-height.
 *
 * Inspired by the fruit-slice swatch cluster in [docs/ideas/swatch_fruit.png].
 *
 * Unlike petal shapes this tile is **wider than it is tall** and uses [uniformScale] = true in
 * [RadialSwatchLayout] so it animates as a whole rather than growing radially.
 */
class CitrusSliceShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(citrusPath(size))

    private fun citrusPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        return Path().apply {
            moveTo(0f, cy) // left tip
            // Outer (convex) arc: left tip → deep downward curve → right tip
            cubicTo(
                w * 0.12f, h * 1.08f, // ctrl 1 — pulls below bounding box
                w * 0.88f, h * 1.08f, // ctrl 2 — mirror
                w, cy, // right tip
            )
            // Inner (concave) arc: right tip → shallow upward curve → left tip
            cubicTo(
                w * 0.80f, -h * 0.12f, // ctrl 1 — pulls above bounding box
                w * 0.20f, -h * 0.12f, // ctrl 2 — mirror
                0f, cy, // back to left tip
            )
            close()
        }
    }
}

/** Citrus-slice / crescent swatch tile — concave inner edge, convex outer edge. */
val CitrusSwatch = CitrusSliceShape()

/**
 * Classic heart shape — two circular lobes at the top, pointed tip at the bottom.
 *
 * Used for the lives indicator on the Threshold game screen.
 * Combine with [androidx.compose.foundation.background] for a solid heart and
 * [androidx.compose.foundation.border] for an outline-only heart.
 */
class HeartShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(heartPath(size))

    private fun heartPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        return Path().apply {
            moveTo(cx, h) // bottom tip
            // Left side — sweeps up through left lobe
            cubicTo(cx * 0.2f, h * 0.75f, 0f, h * 0.45f, 0f, h * 0.30f)
            // Left lobe top arc → center notch
            cubicTo(0f, h * 0.05f, cx * 0.50f, 0f, cx, h * 0.25f)
            // Center notch → right lobe top arc
            cubicTo(cx * 1.50f, 0f, w, h * 0.05f, w, h * 0.30f)
            // Right side — sweeps back down to tip
            cubicTo(w, h * 0.45f, cx * 1.80f, h * 0.75f, cx, h)
            close()
        }
    }
}

/** Heart shape for the Threshold lives indicator. */
val HeartLife = HeartShape()
