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
 * "ZOO" appears as an outline (unlit neon tube), waits, then flickers to life in AccentCyan.
 * A radial bloom behind ZOO pulses with each lit flash.
 * No entrance animation — the flicker IS the moment.
 *
 * Timeline (~3.0 s total):
 *   0 ms    — solid dark background; "HUE" solid + "ZOO" outline appear immediately
 *   500 ms  — pause so the unlit outline registers
 *   500 ms  — ZOO tube-light flicker + bloom pulses (~565 ms)
 *   1065 ms — ZOO settled solid with cyan glow + bloom; tagline fades in
 *   1365 ms — hold (1 200 ms) — lets the settled state fully register
 *   2565 ms — screen fades to black → [onFinished]
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var zooFilled by remember { mutableStateOf(false) }
    val zooGlowAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Wordmark is static — no entrance animation, just wait for eye to register
        delay(500)

        // Tube-light flicker on ZOO — bloom pulses in sync via snapTo
        listOf(
            true to 80L,
            false to 55L,
            true to 50L,
            false to 75L,
            true to 110L,
            false to 40L,
            true to 65L,
            false to 90L,
            true to 0L, // stays lit
        ).forEach { (lit, holdMs) ->
            zooFilled = lit
            zooGlowAlpha.snapTo(if (lit) 1f else 0f)
            if (holdMs > 0L) delay(holdMs)
        }

        // Tagline fades in right after ZOO settles
        launch { taglineAlpha.animateTo(1f, tween(300)) }

        // Hold — give the settled screen time to register before navigating
        delay(1_200)

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
        // Radial bloom pulses with each ZOO flash — Canvas reads state at draw time
        ZooBloom(glowAlpha = { zooGlowAlpha.value })

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

// ── Background bloom ───────────────────────────────────────────────────────────

/**
 * Cyan radial bloom behind ZOO — right-biased to sit behind the "ZOO" half of the wordmark.
 * Pulses with each flicker flash via [glowAlpha]. No other background decoration.
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
            val cx = size.width / 2f + 54.dp.toPx() // right-biased behind ZOO
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
            ZooBloom(glowAlpha = { 1f })
            val base = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                lineHeight = 100.sp,
                fontStyle = FontStyle.Italic,
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
