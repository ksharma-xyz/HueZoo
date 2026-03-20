package xyz.ksharma.huezoo.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewScreen
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

/**
 * Animated logo reveal splash screen.
 *
 * Animation timeline (~2.3 s total):
 *   0 ms     — dark background, all hidden
 *   0–600ms  — cyan radial glow blooms from center; corner brackets, arcs, crosshair appear
 *   250–700ms — "HUEZOO" wordmark springs in (scale 0.85 → 1.0) + fades in
 *   750ms    — tagline "IDENTIFY THE OUTLIER" fades in
 *   1100ms   — hold
 *   1850ms   — glow dims; screen fades to black
 *   2350ms   — [onFinished] called; navigate to Home
 *
 * Back press cannot return to splash — it is removed from the nav back stack on exit.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glowAlpha = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.85f) }
    val taglineAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1 — glow bloom (runs in background)
        launch { glowAlpha.animateTo(1f, tween(600)) }

        delay(250)

        // Phase 2 — wordmark springs in (both in background so we can sequence after)
        launch { logoAlpha.animateTo(1f, tween(450)) }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }

        delay(500)

        // Phase 3 — tagline (awaited so hold doesn't start until it finishes)
        taglineAlpha.animateTo(1f, tween(350))

        // Phase 4 — hold
        delay(750)

        // Phase 5 — fade out then navigate
        launch { glowAlpha.animateTo(0f, tween(400)) }
        screenAlpha.animateTo(0f, tween(500))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
            .background(HuezooColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        SplashIllustration(glowAlpha = { glowAlpha.value })

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // "HUEZOO" — Bebas Neue italic, hero size, EZ pair tighter
            Text(
                text = buildAnnotatedString {
                    append("HU")
                    withStyle(SpanStyle(letterSpacing = (-2).sp)) { append("EZ") }
                    append("OO")
                },
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 96.sp,
                    lineHeight = 100.sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = HuezooColors.AccentCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                },
            )

            Spacer(Modifier.height(HuezooSpacing.md))

            // Tagline
            Text(
                text = "IDENTIFY  THE  OUTLIER",
                style = MaterialTheme.typography.labelMedium,
                color = HuezooColors.TextDisabled,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha.value },
            )
        }
    }
}

// ── Background illustration ───────────────────────────────────────────────────

/**
 * Tactical scanner illustration for the splash — same visual language as
 * [ThresholdScannerIllustration] on the home card.
 *
 * Elements driven by [glowAlpha] (0 → 1):
 *   • Dot grid background
 *   • Radial cyan glow bloom (inner + outer)
 *   • Three concentric partial arcs (scanner rings)
 *   • Military corner brackets
 *   • Crosshair lines with gap around logo
 *   • Two horizontal scan lines with tick marks
 *
 * Takes [glowAlpha] as a lambda so the value is read at draw time (graphicsLayer
 * fast path — no recomposition needed when the value changes).
 */
@Composable
private fun SplashIllustration(
    glowAlpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    val baseColor = HuezooColors.AccentCyan

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val ga = glowAlpha()
        val sw = 1.dp.toPx()

        // ── 1. Dot grid ──────────────────────────────────────────────────────
        val dotStep = 24.dp.toPx()
        val dotR = 1.1.dp.toPx()
        var gx = dotStep / 2f
        while (gx <= w) {
            var gy = dotStep / 2f
            while (gy <= h) {
                drawCircle(
                    color = baseColor.copy(alpha = 0.045f),
                    radius = dotR,
                    center = Offset(gx, gy),
                )
                gy += dotStep
            }
            gx += dotStep
        }

        // ── 2. Radial glow bloom ─────────────────────────────────────────────
        // Outer soft bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = ga * 0.20f),
                    baseColor.copy(alpha = ga * 0.07f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = w * 0.72f,
            ),
            radius = w * 0.72f,
            center = Offset(cx, cy),
        )
        // Inner tighter bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = ga * 0.18f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = w * 0.30f,
            ),
            radius = w * 0.30f,
            center = Offset(cx, cy),
        )

        // ── 3. Concentric partial arcs (scanner rings) ──────────────────────
        listOf(75.dp.toPx(), 140.dp.toPx(), 215.dp.toPx()).forEachIndexed { i, r ->
            val arcAlpha = ga * (0.12f - i * 0.028f).coerceAtLeast(0.02f)
            drawArc(
                color = baseColor.copy(arcAlpha),
                startAngle = -200f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(sw),
            )
            // Tick marks on arcs
            val ticks = 8 + i * 4
            for (t in 0..ticks) {
                val angleDeg = -200f + t * (220f / ticks.toFloat())
                val rad = angleDeg * kotlin.math.PI.toFloat() / 180f
                val cosA = kotlin.math.cos(rad.toDouble()).toFloat()
                val sinA = kotlin.math.sin(rad.toDouble()).toFloat()
                val isMajor = t % 4 == 0
                val tickLen = if (isMajor) 5.dp.toPx() else 2.5.dp.toPx()
                val tickAlpha = (if (isMajor) arcAlpha * 1.7f else arcAlpha).coerceAtMost(0.5f)
                drawLine(
                    baseColor.copy(tickAlpha),
                    Offset(cx + (r - tickLen) * cosA, cy + (r - tickLen) * sinA),
                    Offset(cx + r * cosA, cy + r * sinA),
                    sw,
                )
            }
        }

        // ── 4. Military corner brackets ──────────────────────────────────────
        val bLen = 22.dp.toPx()
        val bAlpha = ga * 0.38f
        val bSw = sw * 2f
        listOf(
            Offset(0f, 0f) to Pair(1f, 1f),
            Offset(w, 0f) to Pair(-1f, 1f),
            Offset(0f, h) to Pair(1f, -1f),
            Offset(w, h) to Pair(-1f, -1f),
        ).forEach { (origin, dir) ->
            drawLine(baseColor.copy(bAlpha), origin, Offset(origin.x + dir.first * bLen, origin.y), bSw)
            drawLine(baseColor.copy(bAlpha), origin, Offset(origin.x, origin.y + dir.second * bLen), bSw)
        }

        // ── 5. Crosshair lines with gap around logo ──────────────────────────
        val crossAlpha = ga * 0.09f
        val gapR = 88.dp.toPx()
        // Horizontal
        drawLine(baseColor.copy(crossAlpha), Offset(0f, cy), Offset(cx - gapR, cy), sw)
        drawLine(baseColor.copy(crossAlpha), Offset(cx + gapR, cy), Offset(w, cy), sw)
        // Vertical
        drawLine(baseColor.copy(crossAlpha), Offset(cx, 0f), Offset(cx, cy - gapR), sw)
        drawLine(baseColor.copy(crossAlpha), Offset(cx, cy + gapR), Offset(cx, h), sw)

        // ── 6. Horizontal scan lines with tick marks ─────────────────────────
        val scanAlpha = ga * 0.06f
        listOf(cy - 110.dp.toPx(), cy + 110.dp.toPx()).forEach { scanY ->
            drawLine(baseColor.copy(scanAlpha), Offset(0f, scanY), Offset(w, scanY), sw)
            var tx = 0f
            while (tx < w) {
                drawLine(
                    baseColor.copy(scanAlpha * 1.8f),
                    Offset(tx, scanY - 3.dp.toPx()),
                    Offset(tx, scanY + 3.dp.toPx()),
                    sw * 0.8f,
                )
                tx += 18.dp.toPx()
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@PreviewScreen
@Composable
private fun SplashPreview() {
    HuezooPreviewTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HuezooColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            // Static preview at mid-animation (glow + logo visible, tagline visible)
            SplashIllustration(glowAlpha = { 1f })
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = buildAnnotatedString {
                        append("HU")
                        withStyle(SpanStyle(letterSpacing = (-2).sp)) { append("EZ") }
                        append("OO")
                    },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 96.sp,
                        lineHeight = 100.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = HuezooColors.AccentCyan,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(HuezooSpacing.md))
                Text(
                    text = "IDENTIFY  THE  OUTLIER",
                    style = MaterialTheme.typography.labelMedium,
                    color = HuezooColors.TextDisabled,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
