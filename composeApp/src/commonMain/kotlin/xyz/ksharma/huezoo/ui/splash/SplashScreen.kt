package xyz.ksharma.huezoo.ui.splash

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
 * Splash screen: static wordmark → ZOO tube-light flicker → settle → outline → flash → out.
 *
 * "HUE" appears solid (the colour is already alive).
 * "ZOO" starts as an outline (unlit neon tube), flickers to life, then smoothly settles.
 * After holding settled, the fill fades back out — ZOO is bare outline again.
 * A sudden white/cyan burst (the tube overloading) snaps on, holds 200 ms, then the
 * flash and screen fade together into the next screen.
 *
 * [zooFillAlpha] drives both the fill layer and the bloom, so flicker frames are
 * instant (snapTo) while the final settle and exit are smooth (animateTo).
 *
 * Timeline (~2.5 s total):
 *   0 ms    — HUE solid + ZOO outline appear; hold so eye registers the unlit state
 *   450 ms  — tube-light flicker sequence (~570 ms, 8 frames)
 *   1020 ms — smooth settle: fill + glow animate to 1.0; tagline fades in
 *   1200 ms — hold settled (600 ms)
 *   1800 ms — fill fades out (100 ms) → ZOO back to bare outline
 *   1930 ms — 130 ms pause — dark / silent
 *   2060 ms — FLASH snaps on (instant)
 *   2260 ms — 200 ms hold
 *   2260 ms — flash fades (350 ms) + screen fades (400 ms) → [onFinished]
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Drives both the ZOO fill layer and the radial bloom.
    // Snapped during flicker, animated for settle and exit.
    val zooFillAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }
    // Full-screen burst at the end — snaps on, then fades out.
    val flashAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // ── 1. Hold so the unlit outline registers ──────────────────────────
        delay(450)

        // ── 2. Tube-light flicker — snapTo for instant on/off ───────────────
        listOf(
            true  to 80L,
            false to 55L,
            true  to 50L,
            false to 75L,
            true  to 110L,
            false to 40L,
            true  to 65L,
            false to 90L,
        ).forEach { (lit, holdMs) ->
            zooFillAlpha.snapTo(if (lit) 1f else 0f)
            delay(holdMs)
        }

        // ── 3. Smooth settle — fill eases in after the last flicker ─────────
        launch { zooFillAlpha.animateTo(1f, tween(130)) }
        // Tagline fades in as ZOO settles
        launch { taglineAlpha.animateTo(1f, tween(400)) }

        // ── 4. Hold settled ─────────────────────────────────────────────────
        delay(700)

        // ── 5. ZOO fades back to outline — fill eases out ───────────────────
        zooFillAlpha.animateTo(0f, tween(110))

        // ── 6. Brief silence — bare outline, no glow, no fill ───────────────
        delay(130)

        // ── 7. FLASH — tube overloads, full-screen burst ─────────────────────
        flashAlpha.snapTo(1f)
        delay(200)

        // ── 8. Flash fades + screen fades together → navigate ───────────────
        launch { flashAlpha.animateTo(0f, tween(350)) }
        screenAlpha.animateTo(0f, tween(400))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
            .background(HuezooColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        // Radial bloom — tied to zooFillAlpha so it pulses with flicker and fades with fill
        ZooBloom(glowAlpha = { zooFillAlpha.value })

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val baseStyle = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                lineHeight = 100.sp,
                fontStyle = FontStyle.Italic,
            )

            Row(verticalAlignment = Alignment.Bottom) {
                // HUE — always solid; the colour is already alive
                Text(
                    text = "HUE",
                    style = baseStyle,
                    color = HuezooColors.AccentCyan,
                )

                // ZOO — two layers:
                //  • bottom: stroke outline (always present — the unlit tube)
                //  • top:    fill + shadow (alpha animated — lights up / dims)
                Box(contentAlignment = Alignment.Center) {
                    // Stroke outline — the bare tube, always visible
                    Text(
                        text = "ZOO",
                        style = baseStyle.copy(drawStyle = Stroke(width = 5f)),
                        color = HuezooColors.AccentCyan,
                    )
                    // Fill with glow — fades in/out via graphicsLayer
                    Text(
                        text = "ZOO",
                        style = baseStyle.copy(
                            drawStyle = Fill,
                            shadow = Shadow(
                                color = HuezooColors.AccentCyan.copy(alpha = 0.85f),
                                blurRadius = 28f,
                                offset = Offset.Zero,
                            ),
                        ),
                        color = HuezooColors.AccentCyan,
                        modifier = Modifier.graphicsLayer { alpha = zooFillAlpha.value },
                    )
                }
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

        // Full-screen flash overlay — white burst centered over ZOO, snaps on then fades
        FlashOverlay(alpha = { flashAlpha.value })
    }
}

// ── Background bloom ───────────────────────────────────────────────────────────

/**
 * Cyan radial bloom behind ZOO — right-biased to sit behind the "ZOO" half of the wordmark.
 * Driven by [glowAlpha] so it pulses with each flicker frame and fades with the fill.
 */
@Composable
private fun ZooBloom(
    glowAlpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    val color = HuezooColors.AccentCyan
    Canvas(modifier = modifier.fillMaxSize()) {
        val zga = glowAlpha()
        if (zga > 0f) {
            val cx = size.width / 2f + 54.dp.toPx()
            val cy = size.height / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = zga * 0.45f),
                        color.copy(alpha = zga * 0.15f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = 120.dp.toPx(),
                ),
                radius = 120.dp.toPx(),
                center = Offset(cx, cy),
            )
        }
    }
}

// ── End-of-sequence flash ──────────────────────────────────────────────────────

/**
 * Full-screen white→cyan radial burst — the "tube overload" moment before the screen
 * clears. Centered slightly right (over ZOO). Drawn on top of everything.
 */
@Composable
private fun FlashOverlay(
    alpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    val cyan = HuezooColors.AccentCyan
    Canvas(modifier = modifier.fillMaxSize()) {
        val a = alpha()
        if (a <= 0f) return@Canvas
        val cx = size.width / 2f + 54.dp.toPx()
        val cy = size.height / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = a),
                    cyan.copy(alpha = a * 0.75f),
                    cyan.copy(alpha = a * 0.25f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = size.maxDimension,
            ),
            radius = size.maxDimension,
            center = Offset(cx, cy),
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewScreen
@Composable
private fun SplashUnlitPreview() {
    HuezooPreviewTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(HuezooColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            val base = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                lineHeight = 100.sp,
                fontStyle = FontStyle.Italic,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text("HUE", style = base, color = HuezooColors.AccentCyan)
                Box(contentAlignment = Alignment.Center) {
                    Text("ZOO", style = base.copy(drawStyle = Stroke(width = 5f)), color = HuezooColors.AccentCyan)
                }
            }
        }
    }
}

@PreviewScreen
@Composable
private fun SplashLitPreview() {
    HuezooPreviewTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(HuezooColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            ZooBloom(glowAlpha = { 1f })
            val base = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                lineHeight = 100.sp,
                fontStyle = FontStyle.Italic,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("HUE", style = base, color = HuezooColors.AccentCyan)
                    Box(contentAlignment = Alignment.Center) {
                        Text("ZOO", style = base.copy(drawStyle = Stroke(width = 5f)), color = HuezooColors.AccentCyan)
                        Text(
                            "ZOO",
                            style = base.copy(
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
                }
                Spacer(Modifier.height(HuezooSpacing.md))
                Text(
                    "IDENTIFY  THE  OUTLIER",
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

@PreviewScreen
@Composable
private fun SplashFlashPreview() {
    HuezooPreviewTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(HuezooColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            val base = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                lineHeight = 100.sp,
                fontStyle = FontStyle.Italic,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text("HUE", style = base, color = HuezooColors.AccentCyan)
                Box(contentAlignment = Alignment.Center) {
                    Text("ZOO", style = base.copy(drawStyle = Stroke(width = 5f)), color = HuezooColors.AccentCyan)
                }
            }
            FlashOverlay(alpha = { 1f })
        }
    }
}
