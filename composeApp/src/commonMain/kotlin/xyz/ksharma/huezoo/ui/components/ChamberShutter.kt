package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize

private val ShutterEasing = CubicBezierEasing(0.6f, 0f, 0.4f, 1f)
private const val SHUTTER_CLOSE_MS = 400
private const val SHUTTER_OPEN_MS = 350
private const val SEAL_BLINK_MS = 1200
private const val SEAL_BLINK_MIN_ALPHA = 0.25f
private val ScanlineSpacing = 6.dp
private const val SCANLINE_ALPHA = 0.9f

/**
 * Animated vertical-scanline shutter that seals a [MemoryChamber].
 *
 * Closes by scaling from the top edge (`scaleY 0 → 1`, 400 ms) and opens in
 * reverse (350 ms) — the Twin Lock "answer key" reveal from the Color Memory
 * Match design handoff. While closed it shows a blinking yellow "◉ SEALED" tag.
 */
@Composable
fun ChamberShutter(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) SHUTTER_CLOSE_MS else SHUTTER_OPEN_MS,
            easing = ShutterEasing,
        ),
        label = "shutterProgress",
    )

    if (progress <= 0.01f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleY = progress
                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
            }
            .border(HuezooSize.BorderThin, HuezooColors.AccentYellow.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        // Vertical scanlines — SurfaceL1 base with SurfaceL2 stripes
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = HuezooColors.SurfaceL1)
            val spacing = ScanlineSpacing.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = HuezooColors.SurfaceL2.copy(alpha = SCANLINE_ALPHA),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = spacing / 2f,
                )
                x += spacing
            }
        }

        // Blinking SEALED tag + HOLD headline
        val blink = rememberInfiniteTransition(label = "sealBlink")
        val blinkAlpha by blink.animateFloat(
            initialValue = 1f,
            targetValue = SEAL_BLINK_MIN_ALPHA,
            animationSpec = infiniteRepeatable(
                animation = tween(SEAL_BLINK_MS / 2, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "sealBlinkAlpha",
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HuezooLabelSmall(
                text = "◉ SEALED",
                color = HuezooColors.AccentYellow.copy(alpha = blinkAlpha),
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            HuezooTitleMedium(
                text = "HOLD",
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}
