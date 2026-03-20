package xyz.ksharma.huezoo.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewScreen
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

/**
 * Animated neon-sign splash screen.
 *
 * Visual concept: "HUE" is always solid bold cyan — the colour is already lit.
 * "ZOO" starts as an outline (unlit tube) then flickers to life like a neon sign.
 *
 * Timeline (~2.8 s total):
 *   0 ms     — dark background, nothing visible
 *   0–500ms  — scanner illustration glows in
 *   250–700ms — "HUEZOO" wordmark springs in; HUE solid, ZOO outlined
 *   700ms    — tube-light flicker sequence on ZOO + background glow pulses
 *   ~1300ms  — ZOO settles solid; tagline fades in
 *   1650ms   — hold
 *   2450ms   — screen fades to black → [onFinished]
 *
 * Back press from Home never returns here — Splash is removed from the back stack on exit.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glowAlpha = remember { Animatable(0f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkScale = remember { Animatable(0.88f) }
    // zooFilled drives the HUE=solid / ZOO=outline vs ZOO=filled split
    var zooFilled by remember { mutableStateOf(false) }
    // zooGlowAlpha controls the bright bloom behind ZOO in the illustration
    val zooGlowAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1 — scanner illustration blooms in
        launch { glowAlpha.animateTo(1f, tween(500)) }

        delay(250)

        // Phase 2 — wordmark springs in (HUE solid, ZOO outline at start)
        launch { wordmarkAlpha.animateTo(1f, tween(400)) }
        launch {
            wordmarkScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }

        delay(700) // wait for wordmark to settle before flicker

        // Phase 3 — tube-light flicker on ZOO
        // Pattern: [lit, holdMs] — each on-flash also pulses the background glow
        val flickers = listOf(
            true  to 80L,
            false to 55L,
            true  to 50L,
            false to 75L,
            true  to 110L,
            false to 40L,
            true  to 65L,
            false to 90L,
            true  to 0L,  // final — stays lit
        )
        flickers.forEach { (lit, holdMs) ->
            zooFilled = lit
            zooGlowAlpha.snapTo(if (lit) 1f else 0f)
            if (holdMs > 0L) delay(holdMs)
        }

        // Phase 4 — tagline appears after ZOO lights up
        delay(200)
        taglineAlpha.animateTo(1f, tween(350))

        // Phase 5 — hold
        delay(800)

        // Phase 6 — fade out and navigate
        launch { glowAlpha.animateTo(0f, tween(400)) }
        launch { zooGlowAlpha.animateTo(0f, tween(400)) }
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
        SplashIllustration(
            glowAlpha = { glowAlpha.value },
            zooGlowAlpha = { zooGlowAlpha.value },
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val baseStyle = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                lineHeight = 100.sp,
                fontStyle = FontStyle.Italic,
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.graphicsLayer {
                    alpha = wordmarkAlpha.value
                    scaleX = wordmarkScale.value
                    scaleY = wordmarkScale.value
                },
            ) {
                // "HUE" — always solid bold fill
                Text(
                    text = "HUE",
                    style = baseStyle,
                    color = HuezooColors.AccentCyan,
                )

                // "ZOO" — outline until flicker sequence completes, then solid + glow
                Text(
                    text = "ZOO",
                    style = if (zooFilled) {
                        baseStyle.copy(
                            drawStyle = Fill,
                            shadow = Shadow(
                                color = HuezooColors.AccentCyan.copy(alpha = 0.85f),
                                blurRadius = 28f,
                                offset = Offset.Zero,
                            ),
                        )
                    } else {
                        baseStyle.copy(drawStyle = Stroke(width = 5f))
                    },
                    color = HuezooColors.AccentCyan,
                )
            }

            Spacer(Modifier.height(HuezooSpacing.md))

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
 * Tactical scanner canvas — dot grid, radial bloom, arcs, corner brackets,
 * crosshair, scan lines. All elements driven by [glowAlpha].
 *
 * [zooGlowAlpha] adds an extra right-biased bloom that pulses with each
 * flicker of the ZOO letters, reinforcing the tube-light effect.
 *
 * Both values taken as lambdas so they're read at draw time — no recomposition
 * overhead during animation frames.
 */
@Composable
private fun SplashIllustration(
    glowAlpha: () -> Float,
    zooGlowAlpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    val baseColor = HuezooColors.AccentCyan

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val ga = glowAlpha()
        val zga = zooGlowAlpha()
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

        // ── 2. Base radial glow bloom (driven by glowAlpha) ──────────────────
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

        // ── 3. ZOO tube-light flash bloom (right-biased, driven by zooGlowAlpha) ──
        // Placed slightly right of center where ZOO letters sit in the wordmark.
        if (zga > 0f) {
            val zooBloomCx = cx + 52.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = zga * 0.45f),
                        baseColor.copy(alpha = zga * 0.15f),
                        Color.Transparent,
                    ),
                    center = Offset(zooBloomCx, cy),
                    radius = 100.dp.toPx(),
                ),
                radius = 100.dp.toPx(),
                center = Offset(zooBloomCx, cy),
            )
        }

        // ── 4. Concentric partial arcs (scanner rings) ───────────────────────
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

        // ── 5. Military corner brackets ──────────────────────────────────────
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

        // ── 6. Crosshair with gap around logo ────────────────────────────────
        val crossAlpha = ga * 0.09f
        val gapR = 88.dp.toPx()
        drawLine(baseColor.copy(crossAlpha), Offset(0f, cy), Offset(cx - gapR, cy), sw)
        drawLine(baseColor.copy(crossAlpha), Offset(cx + gapR, cy), Offset(w, cy), sw)
        drawLine(baseColor.copy(crossAlpha), Offset(cx, 0f), Offset(cx, cy - gapR), sw)
        drawLine(baseColor.copy(crossAlpha), Offset(cx, cy + gapR), Offset(cx, h), sw)

        // ── 7. Horizontal scan lines ──────────────────────────────────────────
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

// ── Previews ──────────────────────────────────────────────────────────────────

/** Preview at initial state: ZOO outlined, HUE solid. */
@PreviewScreen
@Composable
private fun SplashOutlinePreview() {
    HuezooPreviewTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(HuezooColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            SplashIllustration(glowAlpha = { 1f }, zooGlowAlpha = { 0f })
            val baseStyle = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp, lineHeight = 100.sp, fontStyle = FontStyle.Italic,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text("HUE", style = baseStyle, color = HuezooColors.AccentCyan)
                Text("ZOO", style = baseStyle.copy(drawStyle = Stroke(width = 5f)), color = HuezooColors.AccentCyan)
            }
        }
    }
}

/** Preview at final state: both solid, glow active, tagline visible. */
@PreviewScreen
@Composable
private fun SplashLitPreview() {
    HuezooPreviewTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(HuezooColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            SplashIllustration(glowAlpha = { 1f }, zooGlowAlpha = { 1f })
            val baseStyle = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp, lineHeight = 100.sp, fontStyle = FontStyle.Italic,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("HUE", style = baseStyle, color = HuezooColors.AccentCyan)
                    Text(
                        "ZOO",
                        style = baseStyle.copy(
                            drawStyle = Fill,
                            shadow = Shadow(
                                color = HuezooColors.AccentCyan.copy(alpha = 0.85f),
                                blurRadius = 28f,
                                offset = Offset.Zero,
                            ),
                        ),
                        color = HuezooColors.AccentCyan,
                    )
                }
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
