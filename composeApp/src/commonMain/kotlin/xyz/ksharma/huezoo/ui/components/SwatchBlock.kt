package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.SquircleMedium

enum class SwatchBlockSize(val sizeDp: Dp) {
    Small(80.dp),
    Medium(120.dp),
    Large(160.dp),
}

enum class SwatchBlockState {
    Default,
    Pressed,
    Correct,
    Wrong,
    Revealed,
}

/**
 * A colored swatch block used in the game.
 *
 * Animations (baked in):
 * - Appear: scale 0.85 → 1.0, spring bounce (DS.5.5)
 * - Correct tap: scale 1.0 → 1.08 → 1.0 + green border flash (DS.5.6)
 * - Wrong tap: shake ±10 dp × 3 cycles + magenta border (DS.5.1)
 * - Press: scale 0.94 instantly, spring release
 */
@Composable
fun SwatchBlock(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: SwatchBlockSize = SwatchBlockSize.Medium,
    state: SwatchBlockState = SwatchBlockState.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // ── Scale animation ───────────────────────────────────────────────────────
    val scale = remember { Animatable(0.85f) }

    // Appear on first composition
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    // ── Shake animation ───────────────────────────────────────────────────────
    val shakeX = remember { Animatable(0f) }

    LaunchedEffect(state) {
        when (state) {
            SwatchBlockState.Correct -> {
                launch { scale.animateTo(1.08f, tween(100)) }
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            SwatchBlockState.Wrong -> {
                repeat(3) { i ->
                    shakeX.animateTo(
                        targetValue = if (i % 2 == 0) -10f else 10f,
                        animationSpec = tween(durationMillis = 50),
                    )
                }
                shakeX.animateTo(0f, tween(80))
            }
            else -> Unit
        }
    }

    // ── Border ────────────────────────────────────────────────────────────────
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            SwatchBlockState.Correct -> HuezooColors.AccentGreen
            SwatchBlockState.Wrong -> HuezooColors.AccentMagenta
            SwatchBlockState.Pressed -> HuezooColors.AccentCyan.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "borderColor",
    )

    val borderWidth by animateDpAsState(
        targetValue = when (state) {
            SwatchBlockState.Correct, SwatchBlockState.Wrong -> 2.5.dp
            else -> 0.dp
        },
        animationSpec = tween(200),
        label = "borderWidth",
    )

    val pressScale = if (isPressed) 0.94f else 1f

    Box(
        modifier = modifier
            .size(size.sizeDp)
            .graphicsLayer {
                scaleX = scale.value * pressScale
                scaleY = scale.value * pressScale
                translationX = shakeX.value
            }
            .border(borderWidth, borderColor, SquircleMedium)
            .background(color, SquircleMedium)
            .clip(SquircleMedium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = state == SwatchBlockState.Default,
                onClick = onClick,
            ),
    ) {
        SwatchGradientOverlay()
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun SwatchBlockSizesPreview() {
    HuezooPreviewTheme {
        Row {
            SwatchBlock(color = HuezooColors.AccentCyan, onClick = {}, size = SwatchBlockSize.Small)
            Spacer(Modifier.width(12.dp))
            SwatchBlock(color = HuezooColors.AccentPurple, onClick = {}, size = SwatchBlockSize.Medium)
            Spacer(Modifier.width(12.dp))
            SwatchBlock(color = HuezooColors.AccentYellow, onClick = {}, size = SwatchBlockSize.Large)
        }
    }
}

@PreviewComponent
@Composable
private fun SwatchBlockStatesPreview() {
    HuezooPreviewTheme {
        Row {
            SwatchBlock(
                color = HuezooColors.AccentGreen,
                onClick = {},
                state = SwatchBlockState.Correct,
            )
            Spacer(Modifier.width(12.dp))
            SwatchBlock(
                color = HuezooColors.AccentMagenta,
                onClick = {},
                state = SwatchBlockState.Wrong,
            )
        }
    }
}
