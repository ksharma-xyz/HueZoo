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
 * Bulb-petal shape — a wider, more voluminous teardrop than [PetalShape].
 *
 * Differences from the standard petal:
 * - Belly fans out to the full tile width for a rounder silhouette.
 * - Base has a gentle **inward curve** (like a tulip), so the bottom-edge reads as a soft
 *   concave notch rather than a smooth round tip.
 * - No border — shape identity comes from the full, bloated form.
 *
 * Tip sits at (cx, 0) — same radial pivot rule as [PetalShape] applies.
 */
class BulbPetalShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(bulbPetalPath(size))

    private fun bulbPetalPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        return Path().apply {
            moveTo(cx, 0f) // tip — top centre
            // Right edge: sweeps wide early, stays wide through mid-height
            cubicTo(
                w, h * 0.07f, // ctrl 1 — pulls hard right near the top
                w, h * 0.65f, // ctrl 2 — stays at full width
                cx + w * 0.22f, h * 0.88f, // end — bottom-right corner
            )
            // Concave base: right corner → left corner, curves inward
            cubicTo(
                cx + w * 0.10f, h * 1.04f, // ctrl 1 — dips below bounding box (the concavity)
                cx - w * 0.10f, h * 1.04f, // ctrl 2 — mirror on left side
                cx - w * 0.22f, h * 0.88f, // end — bottom-left corner
            )
            // Left edge: sweeps back up symmetrically
            cubicTo(
                0f, h * 0.65f, // ctrl 1 — stays at full width
                0f, h * 0.07f, // ctrl 2 — pulls hard left near the top
                cx, 0f, // end — back to tip
            )
            close()
        }
    }
}

/** Bulb-petal swatch tile — wider and more voluminous than the standard petal. */
val BulbPetalSwatch = BulbPetalShape()

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
 * Rounded shield shape — wide at the outer (bottom) end, tapers to a soft tip at the inner
 * (top) end, with gently concave "shoulders" that give it a pentagon / badge silhouette.
 *
 * Inspired by the swatch cluster in [docs/ideas/swatch_diamong.png].
 *
 * Tip at (cx, 0) — radial pivot math identical to [PetalShape].
 */
class RoundedShieldShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(shieldPath(size))

    private fun shieldPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        return Path().apply {
            moveTo(cx, 0f) // inner tip — top centre
            // Right edge: fans out hard then eases into right shoulder
            cubicTo(
                w, h * 0.12f, // ctrl 1 — pull far right near tip
                w, h * 0.52f, // ctrl 2 — hold full width at mid-height
                w * 0.82f, h * 0.76f, // end — right shoulder
            )
            // Right shoulder → bottom centre arc
            cubicTo(
                w * 0.68f, h * 0.96f, // ctrl 1
                cx + w * 0.16f, h, // ctrl 2
                cx, h, // bottom centre
            )
            // Bottom centre → left shoulder arc (mirror)
            cubicTo(
                cx - w * 0.16f, h, // ctrl 1
                w * 0.32f, h * 0.96f, // ctrl 2
                w * 0.18f, h * 0.76f, // left shoulder
            )
            // Left edge back to tip
            cubicTo(
                0f, h * 0.52f, // ctrl 1
                0f, h * 0.12f, // ctrl 2
                cx, 0f, // back to tip
            )
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
