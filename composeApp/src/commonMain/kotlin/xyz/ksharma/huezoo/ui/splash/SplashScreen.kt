package xyz.ksharma.huezoo.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * Each lit flash cycles through the 5 PlayerLevel colours (Rookie→Master),
 * then settles on AccentCyan to match HUE.
 * No entrance animation — the flicker IS the moment.
 *
 * Timeline (~2.4 s total):
 *   0 ms    — solid dark background; "HUE" solid + "ZOO" outline appear immediately
 *   500 ms  — pause so the unlit outline registers
 *   500 ms  — ZOO tube-light flicker sequence cycling level colours (~565 ms)
 *   1065 ms — ZOO settled solid with glow; tagline fades in immediately
 *   1365 ms — hold
 *   2165 ms — screen fades to black → [onFinished]
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Colours ZOO cycles through during flicker — Rookie → Trained → Sharp → Elite → Master → settle
    val flickerColors = listOf(
        HuezooColors.AccentCyan,      // Rookie
        HuezooColors.AccentGreen,     // Trained
        HuezooColors.AccentMagenta,   // Sharp
        HuezooColors.AccentYellow,    // Elite
        Color(0xFFFFB800),            // Master
        HuezooColors.AccentCyan,      // settle — matches HUE
    )

    var zooFilled by remember { mutableStateOf(false) }
    var zooColor by remember { mutableStateOf(HuezooColors.AccentCyan) }
    val taglineAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Wordmark is static — no entrance animation, just wait for eye to register
        delay(500)

        // Tube-light flicker on ZOO, each lit step gets the next level colour.
        // Tagline starts fading in midway through the flicker so it's already visible when ZOO settles.
        var litStep = 0
        listOf(
            true  to 80L,
            false to 55L,
            true  to 50L,
            false to 75L,   // tagline kicks off here (~265 ms into flicker)
            true  to 110L,
            false to 40L,
            true  to 65L,
            false to 90L,
            true  to 0L,   // stays lit (final settle — AccentCyan)
        ).forEachIndexed { index, (lit, holdMs) ->
            if (lit) {
                zooColor = flickerColors[litStep]
                litStep++
            }
            zooFilled = lit
            // Launch tagline fade partway through (after step 3) so it arrives with ZOO settling
            if (index == 3) launch { taglineAlpha.animateTo(1f, tween(500)) }
            if (holdMs > 0L) delay(holdMs)
        }

        // Hold
        delay(800)

        // Fade out → navigate
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
                                color = zooColor.copy(alpha = 0.85f),
                                blurRadius = 28f,
                                offset = Offset.Zero,
                            ),
                        )
                    } else {
                        baseStyle.copy(drawStyle = Stroke(width = 5f))
                    },
                    color = zooColor,
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
