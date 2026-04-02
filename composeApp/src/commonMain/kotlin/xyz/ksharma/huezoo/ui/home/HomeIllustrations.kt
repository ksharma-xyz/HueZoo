package xyz.ksharma.huezoo.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tactical scanner drawn entirely from lines — GI Joe / robot sensor array aesthetic.
 *
 * Elements:
 *  • Dot grid background (holographic HUD feel)
 *  • 5 concentric partial arcs anchored at the right edge (range rings)
 *  • Tick marks on each arc (major every 4th, minor otherwise)
 *  • Horizontal + vertical crosshair lines with a gap around the focal point
 *  • Radar sweep arm + a ghost trailing arm
 *  • Center focal diamond + cyan dot
 *  • Data blips (small squares) scattered along the outer arcs
 *  • Military corner bracket markers
 *
 * All elements drawn in [HuezooColors.GameThreshold] (indigo-violet) with [HuezooColors.AccentCyan]
 * accents. Alpha drops off toward the left edge, keeping text readable.
 */
@Composable
internal fun ThresholdScannerIllustration(
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val baseColor = HuezooColors.GameThreshold
    val accentColor = LocalPlayerAccentColor.current
    val dimFactor = if (enabled) 1f else 0.35f

    Canvas(modifier = modifier) {
        val sw = 1.dp.toPx()
        val w = size.width
        val h = size.height

        // Reticle anchor: slightly off-screen right, upper-third of card
        val cx = w + 16.dp.toPx()
        val cy = h * 0.28f

        // ── 1. Dot grid ──────────────────────────────────────────────────────
        val dotStep = 22.dp.toPx()
        val dotR = 1.2.dp.toPx()
        var gx = 0f
        while (gx <= w) {
            var gy = 0f
            while (gy <= h) {
                val fadeAlpha = ((gx / w - 0.25f) / 0.75f).coerceIn(0f, 1f)
                drawCircle(
                    color = baseColor.copy(alpha = 0.07f * fadeAlpha * dimFactor),
                    radius = dotR,
                    center = Offset(gx, gy),
                )
                gy += dotStep
            }
            gx += dotStep
        }

        // ── 2. Concentric range arcs ─────────────────────────────────────────
        val arcRadii = listOf(70.0, 115.0, 165.0, 222.0, 285.0).map { it.dp.toPx() }
        val arcStart = 140f
        val arcSweep = 185f

        arcRadii.forEachIndexed { i, r ->
            val arcAlpha = (0.22f - i * 0.025f) * dimFactor
            drawArc(
                color = baseColor.copy(arcAlpha),
                startAngle = arcStart,
                sweepAngle = arcSweep,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = sw),
            )

            val numTicks = 9 + i * 3
            for (t in 0..numTicks) {
                val angleDeg = arcStart + t * (arcSweep / numTicks.toFloat())
                val rad = angleDeg * PI / 180.0
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                val isMajor = t % 4 == 0
                val tickLen = (if (isMajor) 5.5.dp else 2.5.dp).toPx()
                val tickAlpha = (if (isMajor) arcAlpha * 1.8f else arcAlpha).coerceAtMost(0.55f)
                drawLine(
                    color = baseColor.copy(tickAlpha),
                    start = Offset(cx + (r - tickLen) * cosA, cy + (r - tickLen) * sinA),
                    end = Offset(cx + r * cosA, cy + r * sinA),
                    strokeWidth = sw,
                )
            }
        }

        // ── 3. Crosshair lines ────────────────────────────────────────────────
        val crossAlpha = 0.13f * dimFactor
        val gapR = 40.dp.toPx()
        drawLine(baseColor.copy(crossAlpha), Offset(0f, cy), Offset(cx - gapR, cy), sw)
        drawLine(
            baseColor.copy(crossAlpha),
            Offset(cx, cy + gapR),
            Offset(cx, h + 20.dp.toPx()),
            sw,
        )
        drawLine(baseColor.copy(crossAlpha), Offset(cx, 0f), Offset(cx, cy - gapR), sw)

        // ── 4. Radar sweep arm + ghost ────────────────────────────────────────
        val sweepDeg = 212.0
        val sweepRad = sweepDeg * PI / 180.0
        val sweepLen = arcRadii.last()
        drawLine(
            color = baseColor.copy(0.30f * dimFactor),
            start = Offset(cx, cy),
            end = Offset(
                (cx + sweepLen * cos(sweepRad)).toFloat(),
                (cy + sweepLen * sin(sweepRad)).toFloat(),
            ),
            strokeWidth = sw * 1.6f,
        )
        val ghostRad = (sweepDeg - 14.0) * PI / 180.0
        drawLine(
            color = baseColor.copy(0.10f * dimFactor),
            start = Offset(cx, cy),
            end = Offset(
                (cx + sweepLen * cos(ghostRad)).toFloat(),
                (cy + sweepLen * sin(ghostRad)).toFloat(),
            ),
            strokeWidth = sw,
        )

        // ── 5. Center focal diamond + accent dot ──────────────────────────────
        val d = 9.dp.toPx()
        val diamond = Path().apply {
            moveTo(cx, cy - d)
            lineTo(cx + d, cy)
            lineTo(cx, cy + d)
            lineTo(cx - d, cy)
            close()
        }
        drawPath(diamond, baseColor.copy(0.45f * dimFactor), style = Stroke(sw * 1.5f))
        drawCircle(accentColor.copy(0.80f * dimFactor), 2.5.dp.toPx(), Offset(cx, cy))

        // ── 6. Data blips on arcs ─────────────────────────────────────────────
        data class Blip(val arcIdx: Int, val angleDeg: Float)

        val blips = listOf(
            Blip(2, 148f), Blip(2, 182f), Blip(2, 225f), Blip(2, 275f), Blip(2, 318f),
            Blip(3, 155f), Blip(3, 200f), Blip(3, 260f), Blip(3, 310f),
            Blip(4, 170f), Blip(4, 240f), Blip(4, 290f),
        )
        blips.forEach { (arcIdx, angleDeg) ->
            val r = arcRadii[arcIdx]
            val rad = angleDeg * PI / 180.0
            val bx = (cx + r * cos(rad)).toFloat()
            val by = (cy + r * sin(rad)).toFloat()
            if (bx < w && by >= 0f && by <= h) {
                val bSz = 4.dp.toPx()
                val isAccent = angleDeg in 180f..200f || angleDeg in 300f..320f
                val blipColor = if (isAccent) accentColor else baseColor
                val blipAlpha = if (isAccent) 0.60f * dimFactor else 0.35f * dimFactor
                drawRect(
                    blipColor.copy(blipAlpha),
                    Offset(bx - bSz / 2f, by - bSz / 2f),
                    Size(bSz, bSz),
                )
            }
        }

        // ── 7. Military corner brackets ───────────────────────────────────────
        val bLen = 12.dp.toPx()
        val bAlpha = 0.22f * dimFactor
        val bSw = sw * 1.8f

        data class Corner(val x: Float, val y: Float, val dx: Float, val dy: Float)
        listOf(
            Corner(0f, 0f, 1f, 1f),
            Corner(w, 0f, -1f, 1f),
            Corner(0f, h, 1f, -1f),
            Corner(w, h, -1f, -1f),
        ).forEach { (x, y, dx, dy) ->
            drawLine(baseColor.copy(bAlpha), Offset(x, y), Offset(x + dx * bLen, y), bSw)
            drawLine(baseColor.copy(bAlpha), Offset(x, y), Offset(x, y + dy * bLen), bSw)
        }

        // ── 8. Horizontal data-scan line at 60% height ───────────────────────
        val scanY = h * 0.62f
        drawLine(
            color = accentColor.copy(0.08f * dimFactor),
            start = Offset(w * 0.35f, scanY),
            end = Offset(w, scanY),
            strokeWidth = sw,
        )
        var tx = w * 0.40f
        while (tx <= w - 4.dp.toPx()) {
            drawLine(
                accentColor.copy(0.12f * dimFactor),
                Offset(tx, scanY - 3.dp.toPx()),
                Offset(tx, scanY + 3.dp.toPx()),
                sw,
            )
            tx += 18.dp.toPx()
        }
    }
}

/**
 * A pile of faceted gem diamonds spilling in from the right — decorative illustration
 * for the inventory card.
 *
 * Layout: one large hero gem + several medium/small gems scattered around it,
 * all anchored to the right half so the left-side text stays readable.
 */
@Composable
internal fun GemSpillIllustration(modifier: Modifier = Modifier) {
    val color = LocalPlayerAccentColor.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 0.9.dp.toPx()

        val anchorX = w * 0.78f
        val anchorY = h * 0.5f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.09f), Color.Transparent),
                center = Offset(anchorX, anchorY),
                radius = h * 1.1f,
            ),
            radius = h * 1.1f,
            center = Offset(anchorX, anchorY),
        )

        data class GemSpec(val dx: Float, val dy: Float, val size: Float, val rotDeg: Float)

        val specs = listOf(
            GemSpec(0f, 0f, h * 0.72f, 0f),
            GemSpec(-h * 0.55f, h * 0.10f, h * 0.42f, -18f),
            GemSpec(-h * 0.30f, -h * 0.40f, h * 0.32f, 22f),
            GemSpec(h * 0.28f, -h * 0.38f, h * 0.26f, -30f),
            GemSpec(h * 0.22f, h * 0.35f, h * 0.22f, 14f),
            GemSpec(-h * 0.65f, -h * 0.25f, h * 0.18f, 35f),
            GemSpec(h * 0.08f, -h * 0.55f, h * 0.14f, -10f),
        )

        specs.forEachIndexed { idx, spec ->
            val cx = anchorX + spec.dx
            val cy = anchorY + spec.dy
            val s = spec.size
            if (cx + s < w * 0.38f) return@forEachIndexed
            val alpha = when (idx) {
                0 -> 0.18f
                1 -> 0.13f
                2 -> 0.11f
                else -> 0.08f
            }

            val rotRad = spec.rotDeg * PI.toFloat() / 180f
            val cosR = cos(rotRad)
            val sinR = sin(rotRad)

            fun rotated(px: Float, py: Float): Offset =
                Offset(cx + px * cosR - py * sinR, cy + px * sinR + py * cosR)

            val top = rotated(0f, -s * 0.5f)
            val upR = rotated(s * 0.40f, -s * 0.14f)
            val loR = rotated(s * 0.28f, s * 0.12f)
            val bot = rotated(0f, s * 0.5f)
            val loL = rotated(-s * 0.28f, s * 0.12f)
            val upL = rotated(-s * 0.40f, -s * 0.14f)

            val gemPath = Path().apply {
                moveTo(top.x, top.y)
                lineTo(upR.x, upR.y)
                lineTo(loR.x, loR.y)
                lineTo(bot.x, bot.y)
                lineTo(loL.x, loL.y)
                lineTo(upL.x, upL.y)
                close()
            }
            drawPath(gemPath, color.copy(alpha = alpha))
            drawPath(gemPath, color.copy(alpha = alpha * 1.5f), style = Stroke(width = sw))

            val girL = rotated(-s * 0.20f, -s * 0.14f)
            val girR = rotated(s * 0.20f, -s * 0.14f)
            val facetAlpha = (alpha * 1.8f).coerceAtMost(0.45f)
            drawLine(color.copy(facetAlpha), top, girL, sw * 0.8f)
            drawLine(color.copy(facetAlpha), top, girR, sw * 0.8f)
            drawLine(color.copy(facetAlpha), girL, girR, sw * 0.8f)

            val girdleCentre = rotated(0f, -s * 0.14f)
            drawLine(color.copy(alpha * 1.4f), girdleCentre, bot, sw * 0.7f)
        }

        data class Sparkle(val x: Float, val y: Float, val r: Float)
        val sparkles = listOf(
            Sparkle(anchorX - h * 0.42f, anchorY - h * 0.58f, h * 0.055f),
            Sparkle(anchorX + h * 0.18f, anchorY - h * 0.50f, h * 0.040f),
            Sparkle(anchorX + h * 0.35f, anchorY + h * 0.30f, h * 0.035f),
            Sparkle(anchorX - h * 0.72f, anchorY + h * 0.20f, h * 0.030f),
            Sparkle(anchorX - h * 0.10f, anchorY + h * 0.52f, h * 0.028f),
        )
        sparkles.forEach { (sx, sy, r) ->
            if (sx < w * 0.42f) return@forEach
            val a = 0.20f
            drawLine(color.copy(a), Offset(sx - r, sy), Offset(sx + r, sy), sw * 0.8f)
            drawLine(color.copy(a), Offset(sx, sy - r), Offset(sx, sy + r), sw * 0.8f)
            val dr = r * 0.55f
            drawLine(color.copy(a * 0.6f), Offset(sx - dr, sy - dr), Offset(sx + dr, sy + dr), sw * 0.6f)
            drawLine(color.copy(a * 0.6f), Offset(sx - dr, sy + dr), Offset(sx + dr, sy - dr), sw * 0.6f)
        }

        val scanY = h * 0.68f
        drawLine(
            color.copy(alpha = 0.07f),
            Offset(w * 0.45f, scanY),
            Offset(w, scanY),
            sw * 0.8f,
        )
        var scanTx = w * 0.50f
        while (scanTx < w - 2.dp.toPx()) {
            drawLine(color.copy(0.10f), Offset(scanTx, scanY - 2.5.dp.toPx()), Offset(scanTx, scanY + 2.5.dp.toPx()), sw * 0.7f)
            scanTx += 16.dp.toPx()
        }
    }
}

/**
 * Neon cat that bobs up/down and wags its tail — shown in the Daily Challenge
 * compact card icon box when today's challenge is still available to play.
 */
@Composable
internal fun AnimatedDailyIcon(color: Color) {
    val transition = rememberInfiniteTransition(label = "dailyCat")

    val bobY by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val tailAngle by transition.animateFloat(
        initialValue = -22f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tail",
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Canvas(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer { translationY = bobY },
    ) {
        drawNeonCat(color, tailAngle, glowAlpha)
    }
}

@Suppress("MagicNumber")
private fun DrawScope.drawNeonCat(color: Color, tailAngleDeg: Float, glowAlpha: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.54f
    val headR = size.minDimension * 0.27f
    val sw = 1.5.dp.toPx()

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = glowAlpha * 0.45f), Color.Transparent),
            center = Offset(cx, cy),
            radius = headR * 2.3f,
        ),
        radius = headR * 2.3f,
        center = Offset(cx, cy),
    )

    val leftEar = Path().apply {
        moveTo(cx - headR * 0.55f, cy - headR * 0.82f)
        lineTo(cx - headR * 0.90f, cy - headR * 1.52f)
        lineTo(cx - headR * 0.15f, cy - headR * 1.10f)
        close()
    }
    drawPath(leftEar, color.copy(alpha = 0.85f))

    val rightEar = Path().apply {
        moveTo(cx + headR * 0.55f, cy - headR * 0.82f)
        lineTo(cx + headR * 0.90f, cy - headR * 1.52f)
        lineTo(cx + headR * 0.15f, cy - headR * 1.10f)
        close()
    }
    drawPath(rightEar, color.copy(alpha = 0.85f))

    drawCircle(color = color.copy(alpha = 0.10f), radius = headR, center = Offset(cx, cy))
    drawCircle(color = color, radius = headR, center = Offset(cx, cy), style = Stroke(sw))

    val eyeR = headR * 0.14f
    drawCircle(color, eyeR, Offset(cx - headR * 0.33f, cy - headR * 0.08f))
    drawCircle(color, eyeR, Offset(cx + headR * 0.33f, cy - headR * 0.08f))
    drawCircle(color.copy(alpha = 0.65f), headR * 0.07f, Offset(cx, cy + headR * 0.22f))

    val tailPivotX = cx + headR * 0.40f
    val tailPivotY = cy + headR * 0.72f
    withTransform({
        rotate(degrees = tailAngleDeg, pivot = Offset(tailPivotX, tailPivotY))
    }) {
        val tailPath = Path().apply {
            moveTo(tailPivotX, tailPivotY)
            cubicTo(
                tailPivotX + headR * 0.85f,
                tailPivotY + headR * 0.50f,
                tailPivotX + headR * 1.25f,
                tailPivotY + headR * 0.08f,
                tailPivotX + headR * 1.15f,
                tailPivotY - headR * 0.45f,
            )
        }
        drawPath(tailPath, color.copy(alpha = 0.80f), style = Stroke(sw, cap = StrokeCap.Round))
    }
}

