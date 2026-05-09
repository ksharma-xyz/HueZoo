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

    @Suppress("MagicNumber")
    private fun shieldPath(size: Size): Path {
        val cx = size.width / 2f
        // Widest point below mid-height → inner portion is taller (more pointed inner tip)
        // while outer portion stays at roughly the same angle as a symmetric diamond.
        val sidePtY = size.height * 0.58f
        return Path().apply {
            moveTo(cx, 0f) // inner tip — top centre, points toward layout centre
            lineTo(size.width, sidePtY) // right point — widest edge (58 % down)
            lineTo(cx, size.height) // outer tip — bottom centre, points away from centre
            lineTo(0f, sidePtY) // left point — widest edge
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

    @Suppress("MagicNumber")
    private fun citrusPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        return Path().apply {
            moveTo(0f, cy) // left tip
            // Outer (convex) arc: left tip → deep downward curve → right tip
            cubicTo(
                w * 0.12f,
                h * 1.08f, // ctrl 1 — pulls below bounding box
                w * 0.88f,
                h * 1.08f, // ctrl 2 — mirror
                w,
                cy, // right tip
            )
            // Inner (concave) arc: right tip → shallow upward curve → left tip
            cubicTo(
                w * 0.80f,
                -h * 0.12f, // ctrl 1 — pulls above bounding box
                w * 0.20f,
                -h * 0.12f, // ctrl 2 — mirror
                0f,
                cy, // back to left tip
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

/**
 * Heart silhouette for the radial swatch layout — flipped from [HeartShape] so the sharp
 * tip points to the **inner** edge (centre) and the plump rounded lobes face outward.
 * Six tiles arranged radially form a flower of hearts with their tips converging at a
 * single hub and lobes blooming around the rim.
 */
class HeartSwatchShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(heartSwatchPath(size))

    @Suppress("MagicNumber", "LongMethod")
    private fun heartSwatchPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        return Path().apply {
            // Sharp inner tip at top.
            moveTo(cx, 0f)
            // Right side — sweeps from tip down to the right widest point.
            cubicTo(
                w * 0.55f,
                h * 0.04f,
                w * 1.02f,
                h * 0.20f,
                w,
                h * 0.55f,
            )
            // Right lobe — plump rounded curve down to the centre notch at the outer edge.
            cubicTo(
                w,
                h * 0.92f,
                w * 0.70f,
                h,
                cx,
                h * 0.86f,
            )
            // Left lobe — mirror back to the left widest point.
            cubicTo(
                w * 0.30f,
                h,
                0f,
                h * 0.92f,
                0f,
                h * 0.55f,
            )
            // Left side — back up to the inner tip.
            cubicTo(
                w * -0.02f,
                h * 0.20f,
                w * 0.45f,
                h * 0.04f,
                cx,
                0f,
            )
            close()
        }
    }
}

/** Heart tile for the radial swatch layout — tip points inward, lobes outward. */
val HeartSwatch = HeartSwatchShape()

/**
 * Carrot silhouette — frilly leafy inner edge (3-peak zigzag crown at the top) tapering to a
 * narrow pointed tip at the outer end.  Both side edges are gently curved (slight outward
 * bow) so the carrot reads as plump rather than a flat triangle.
 */
class CarrotShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(carrotPath(size))

    @Suppress("MagicNumber")
    private fun carrotPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val frillBaseY = h * 0.18f
        return Path().apply {
            moveTo(0f, frillBaseY)
            lineTo(w * 0.17f, 0f)
            lineTo(w * 0.33f, frillBaseY)
            lineTo(w * 0.50f, 0f)
            lineTo(w * 0.67f, frillBaseY)
            lineTo(w * 0.83f, 0f)
            lineTo(w, frillBaseY)
            cubicTo(
                w * 0.96f,
                h * 0.55f,
                w * 0.74f,
                h * 0.92f,
                cx,
                h,
            )
            cubicTo(
                w * 0.26f,
                h * 0.92f,
                w * 0.04f,
                h * 0.55f,
                0f,
                frillBaseY,
            )
            close()
        }
    }
}

/** Carrot tile for the Carrot swatch layout. */
val CarrotSwatch = CarrotShape()

/**
 * Alloy-wheel arrow tile — pentagonal arrowhead with a chevron notch carved into the outer
 * edge.  Inner tip points toward the layout centre; outer edge has a V-shaped indent.
 */
class AlloyArrowShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(alloyPath(size))

    @Suppress("MagicNumber")
    private fun alloyPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val shoulderY = h * 0.58f
        val notchInY = h * 0.78f
        return Path().apply {
            moveTo(cx, 0f)
            lineTo(w, shoulderY)
            lineTo(w, h)
            lineTo(cx, notchInY)
            lineTo(0f, h)
            lineTo(0f, shoulderY)
            close()
        }
    }
}

/** Alloy-spoke arrowhead tile. */
val AlloyArrowSwatch = AlloyArrowShape()

/**
 * Y-spoke alloy tile — twin-fork silhouette.  Narrow stem at the inner edge widens at the
 * shoulders into two prongs separated by a deep V-notch at the outer edge.  Audi-style.
 */
class YSpokeShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(yPath(size))

    @Suppress("MagicNumber")
    private fun yPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val shoulderY = h * 0.32f
        val notchInY = h * 0.55f
        return Path().apply {
            moveTo(cx, 0f)
            lineTo(w, shoulderY)
            lineTo(w, h)
            lineTo(cx, notchInY)
            lineTo(0f, h)
            lineTo(0f, shoulderY)
            close()
        }
    }
}

/** Y-spoke twin-fork alloy tile. */
val YSpokeSwatch = YSpokeShape()

/**
 * Trident-spoke alloy tile — three flat outer prongs separated by two V-notches.
 * Mercedes-AMG split-spoke aesthetic.  9 vertices.
 */
class TridentSpokeShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(tridentPath(size))

    @Suppress("MagicNumber", "LongMethod")
    private fun tridentPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val shoulderY = h * 0.50f
        val notchInY = h * 0.78f
        return Path().apply {
            // Sharp inner tip.
            moveTo(cx, 0f)
            // Right side — gentle convex sweep from tip down to the shoulder (was a straight
            // line; the curve makes it feel sculpted rather than cleavered).
            cubicTo(
                w * 0.78f,
                h * 0.10f,
                w * 1.02f,
                h * 0.30f,
                w,
                shoulderY,
            )
            lineTo(w, h) // outer-right prong corner
            lineTo(w * 0.67f, notchInY) // right notch apex
            lineTo(w * 0.55f, h) // mid-prong right base
            lineTo(w * 0.45f, h) // mid-prong left base
            lineTo(w * 0.33f, notchInY) // left notch apex
            lineTo(0f, h) // outer-left prong corner
            // Left side — mirror back up to the inner tip.
            cubicTo(
                w * -0.02f,
                h * 0.30f,
                w * 0.22f,
                h * 0.10f,
                cx,
                0f,
            )
            close()
        }
    }
}

/** Trident 3-prong alloy spoke tile. */
val TridentSpokeSwatch = TridentSpokeShape()

/**
 * Cutout-chevron alloy tile — same 6-corner anchor template as [AlloyArrowShape] but with
 * concave (cubic) sweeps between shoulders and outer corners on both sides — sharp
 * jet-wing silhouette.
 */
class CutoutChevronShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(chevronPath(size))

    @Suppress("MagicNumber")
    private fun chevronPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val shoulderY = h * 0.55f
        val notchInY = h * 0.78f
        return Path().apply {
            moveTo(cx, 0f)
            lineTo(w, shoulderY)
            cubicTo(
                w * 0.92f,
                h * 0.72f,
                w * 0.88f,
                h * 0.92f,
                w,
                h,
            )
            lineTo(cx, notchInY)
            lineTo(0f, h)
            cubicTo(
                w * 0.12f,
                h * 0.92f,
                w * 0.08f,
                h * 0.72f,
                0f,
                shoulderY,
            )
            close()
        }
    }
}

/** Cutout-chevron jet-wing alloy tile. */
val CutoutChevronSwatch = CutoutChevronShape()

/**
 * Mjolnir-hammer silhouette — narrow rounded handle at the inner edge, filleted shoulders
 * curving smoothly out into a chamfered hammer head at the outer edge.  Head body is 84 % of
 * tile width with cubic-rounded outer corners.
 */
class MjolnirShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(mjolnirPath(size))

    @Suppress("MagicNumber", "LongMethod")
    private fun mjolnirPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val handleHalfW = w * 0.11f
        val gripDomeY = h * 0.05f
        val handleBaseY = h * 0.30f
        val shoulderEndY = h * 0.42f
        val headHalfW = w * 0.42f
        val cornerR = w * 0.07f
        return Path().apply {
            moveTo(cx - handleHalfW, gripDomeY)
            cubicTo(
                cx - handleHalfW,
                0f,
                cx + handleHalfW,
                0f,
                cx + handleHalfW,
                gripDomeY,
            )
            lineTo(cx + handleHalfW, handleBaseY)
            cubicTo(
                cx + handleHalfW,
                shoulderEndY,
                cx + headHalfW,
                handleBaseY,
                cx + headHalfW,
                shoulderEndY,
            )
            lineTo(cx + headHalfW, h - cornerR)
            cubicTo(
                cx + headHalfW,
                h,
                cx + headHalfW - cornerR * 0.45f,
                h,
                cx + headHalfW - cornerR,
                h,
            )
            lineTo(cx - headHalfW + cornerR, h)
            cubicTo(
                cx - headHalfW + cornerR * 0.45f,
                h,
                cx - headHalfW,
                h,
                cx - headHalfW,
                h - cornerR,
            )
            lineTo(cx - headHalfW, shoulderEndY)
            cubicTo(
                cx - headHalfW,
                handleBaseY,
                cx - handleHalfW,
                shoulderEndY,
                cx - handleHalfW,
                handleBaseY,
            )
            lineTo(cx - handleHalfW, gripDomeY)
            close()
        }
    }
}

/** Mjolnir-hammer tile. */
val MjolnirSwatch = MjolnirShape()

/**
 * Classic 5-point star — alternating outer / inner radii on an ellipse fitted to the tile
 * bounding box.  The top outer point sits at (cx, 0) so it faces the layout centre.
 */
class StarShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(starPath(size))

    @Suppress("MagicNumber")
    private fun starPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val rxOuter = w * 0.48f
        val ryOuter = h * 0.48f
        // Inner radius is 70 % of outer (was 40 %) — chunkier, less spiky, kid-friendlier.
        val rxInner = w * 0.34f
        val ryInner = h * 0.34f
        return Path().apply {
            for (i in 0..9) {
                val angle = -PI / 2.0 + i * PI / 5.0
                val rx = if (i % 2 == 0) rxOuter else rxInner
                val ry = if (i % 2 == 0) ryOuter else ryInner
                val px = (cx + rx * cos(angle)).toFloat()
                val py = (cy + ry * sin(angle)).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
    }
}

/** 5-point star tile. */
val StarSwatch = StarShape()

/**
 * Toy-kite silhouette — elongated rhombus with side points at 32 % from the inner edge.
 */
class KiteShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(kitePath(size))

    @Suppress("MagicNumber")
    private fun kitePath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        return Path().apply {
            moveTo(cx, 0f)
            lineTo(w, h * 0.32f)
            lineTo(cx, h)
            lineTo(0f, h * 0.32f)
            close()
        }
    }
}

/** Toy-kite tile. */
val KiteSwatch = KiteShape()

/**
 * Three-dome crown silhouette — three rounded arches across the inner edge (no sharp peaks),
 * flat outer base.  The centre arch is the tallest; the two side arches are shorter.
 */
class CrownShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(crownPath(size))

    @Suppress("MagicNumber", "LongMethod")
    private fun crownPath(size: Size): Path {
        val w = size.width
        val h = size.height
        val sideArchTop = h * 0.10f
        val midArchTop = 0f
        val valleyY = h * 0.45f
        return Path().apply {
            moveTo(0f, valleyY)
            // Left arch — dome up to (w*0.33, valleyY).
            cubicTo(
                0f,
                sideArchTop,
                w * 0.33f,
                sideArchTop,
                w * 0.33f,
                valleyY,
            )
            // Centre arch — taller; reaches y = 0 at its peak.
            cubicTo(
                w * 0.33f,
                midArchTop,
                w * 0.67f,
                midArchTop,
                w * 0.67f,
                valleyY,
            )
            // Right arch — mirror of the left.
            cubicTo(
                w * 0.67f,
                sideArchTop,
                w,
                sideArchTop,
                w,
                valleyY,
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
    }
}

/** Three-dome crown tile. */
val CrownSwatch = CrownShape()

/**
 * Apple-slice silhouette — sharp inner tip (where the apple's core was), straight cut
 * sides, gently convex skin curve at the outer edge.  Six slices arranged radially form a
 * chef's-plate of apple slices.
 */
class AppleShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(applePath(size))

    @Suppress("MagicNumber")
    private fun applePath(size: Size): Path {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val skinAnchorY = h * 0.65f
        return Path().apply {
            moveTo(cx, 0f) // sharp inner tip (core point)
            lineTo(w * 0.95f, skinAnchorY) // right cut edge
            // Apple skin — gentle convex sweep across the outer edge.
            cubicTo(
                w * 0.85f,
                h * 1.02f,
                w * 0.15f,
                h * 1.02f,
                w * 0.05f,
                skinAnchorY,
            )
            lineTo(cx, 0f) // left cut edge back to tip
            close()
        }
    }
}

/** Apple-slice tile. */
val AppleSwatch = AppleShape()
