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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Splash screen: static wordmark → ZOO tube-light flicker → full lit.
 *
 * "HUE" appears solid (the colour is already alive).
 * "ZOO" appears as an outline (unlit neon tube), waits, then flickers to life.
 * No entrance animation — the flicker IS the moment.
 *
 * Timeline (~2.8 s total):
 *   0 ms    — solid dark background; corner brackets fade in
 *   0 ms    — "HUE" solid + "ZOO" outline appear immediately (static)
 *   500 ms  — pause so the unlit outline registers
 *   500 ms  — ZOO tube-light flicker sequence (~650 ms)
 *   1150 ms — ZOO settled solid with cyan glow; tagline fades in
 *   1500 ms — hold
 *   2300 ms — screen fades to black → [onFinished]
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bracketsAlpha = remember { Animatable(0f) }
    var zooFilled by remember { mutableStateOf(false) }
    val zooGlowAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Corner brackets fade in immediately
        launch { bracketsAlpha.animateTo(1f, tween(400)) }

        // Wordmark is static — no entrance animation, just wait for eye to register
        delay(500)

        // Tube-light flicker on ZOO
        listOf(
            true  to 80L,
            false to 55L,
            true  to 50L,
            false to 75L,
            true  to 110L,
            false to 40L,
            true  to 65L,
            false to 90L,
            true  to 0L,   // stays lit
        ).forEach { (lit, holdMs) ->
            zooFilled = lit
            zooGlowAlpha.snapTo(if (lit) 1f else 0f)
            if (holdMs > 0L) delay(holdMs)
        }

        // Tagline after ZOO is lit
        delay(150)
        taglineAlpha.animateTo(1f, tween(300))

        // Hold
        delay(800)

        // Fade out → navigate
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
        SplashBackground(
            bracketsAlpha = { bracketsAlpha.value },
            zooGlowAlpha = { zooGlowAlpha.value },
        )

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
                // ZOO — outline (unlit tube) until flicker completes, then solid + glow
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

// ── Background canvas ─────────────────────────────────────────────────────────

/**
 * Minimal solid-background canvas — dots, corner brackets, and the ZOO flicker bloom.
 * No radial glow blooms or arcs; keeps the background truly solid and uncluttered
 * so the wordmark and flicker stay in focus.
 */
@Composable
private fun SplashBackground(
    bracketsAlpha: () -> Float,
    zooGlowAlpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    val color = HuezooColors.AccentCyan

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val sw = 1.dp.toPx()
        val ba = bracketsAlpha()
        val zga = zooGlowAlpha()

        // ── Dot grid — very subtle, always on ────────────────────────────────
        val step = 26.dp.toPx()
        val dotR = 1.dp.toPx()
        var gx = step / 2f
        while (gx <= w) {
            var gy = step / 2f
            while (gy <= h) {
                drawCircle(color.copy(alpha = 0.04f), dotR, Offset(gx, gy))
                gy += step
            }
            gx += step
        }

        // ── Military corner brackets — fade in with bracketsAlpha ─────────────
        val bLen = 24.dp.toPx()
        val bAlpha = ba * 0.40f
        val bSw = sw * 2.2f
        listOf(
            Offset(0f, 0f) to Pair(1f, 1f),
            Offset(w, 0f) to Pair(-1f, 1f),
            Offset(0f, h) to Pair(1f, -1f),
            Offset(w, h) to Pair(-1f, -1f),
        ).forEach { (o, d) ->
            drawLine(color.copy(bAlpha), o, Offset(o.x + d.first * bLen, o.y), bSw)
            drawLine(color.copy(bAlpha), o, Offset(o.x, o.y + d.second * bLen), bSw)
        }

        // ── ZOO flicker bloom — right-biased, pulses with each flash ─────────
        if (zga > 0f) {
            val boomCx = cx + 54.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = zga * 0.40f),
                        color.copy(alpha = zga * 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(boomCx, cy),
                    radius = 110.dp.toPx(),
                ),
                radius = 110.dp.toPx(),
                center = Offset(boomCx, cy),
            )
        }
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
            SplashBackground(bracketsAlpha = { 1f }, zooGlowAlpha = { 0f })
            val base = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp, lineHeight = 100.sp, fontStyle = FontStyle.Italic,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text("HUE", style = base, color = HuezooColors.AccentCyan)
                Text("ZOO", style = base.copy(drawStyle = Stroke(width = 5f)), color = HuezooColors.AccentCyan)
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
            SplashBackground(bracketsAlpha = { 1f }, zooGlowAlpha = { 1f })
            val base = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp, lineHeight = 100.sp, fontStyle = FontStyle.Italic,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("HUE", style = base, color = HuezooColors.AccentCyan)
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
