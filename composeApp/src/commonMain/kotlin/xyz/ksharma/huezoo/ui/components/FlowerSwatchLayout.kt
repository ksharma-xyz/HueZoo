package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.games.threshold.state.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.model.SwatchUiModel
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.SwatchPetal

// ── Flower layout dimensions ──────────────────────────────────────────────────

private val CONTAINER_SIZE = 300.dp
private val PETAL_WIDTH = 90.dp
private val PETAL_HEIGHT = 128.dp

private const val PETAL_COUNT = 6
private const val STAGGER_UNFOLD_MS = 72L  // delay between each petal opening
private const val STAGGER_FOLD_MS = 48L    // delay between each petal closing (faster)
private const val FOLD_DURATION_MS = 220
private const val BORDER_STROKE_DP = 4f
private const val NEON_OUTER_ALPHA = 0.30f
private const val NEON_OUTER_EXTRA_PX = 8f
private const val NEON_INNER_EXTRA_PX = 3f

/**
 * Renders [swatches] (exactly 6) as a radial flower with petal-shaped tiles.
 *
 * ## Layout geometry
 * Each petal has its **pointed tip at the top** of the petal bounding box.
 * Petals are positioned so their tip sits at the container's centre point, then each is
 * rotated by `index × 60°` around `TransformOrigin(0.5f, 0f)` (the tip pivot).
 * This fans the petals outward evenly to form a 6-petal flower.
 *
 * ## Animation contract
 * | Trigger | Effect |
 * |---------|--------|
 * | `roundKey` changes | All petals snap to 0 then stagger-unfold (scaleY 0 → 1, from tip) |
 * | `roundPhase == FoldingOut` | All petals stagger-fold (scaleY 1 → 0, reverse order) |
 * | `SwatchDisplayState.Correct` | Green border + faint neon glow on that petal |
 * | `SwatchDisplayState.Wrong` | Magenta border + horizontal shake on that petal |
 * | `SwatchDisplayState.Revealed` | Cyan border + faint neon glow on that petal |
 *
 * ## Interaction
 * Only petals in `Default` state are tappable. `FoldingOut`, `Correct`, `Wrong`, `Revealed` are
 * all non-interactive.
 */
@Composable
fun FlowerSwatchLayout(
    swatches: List<SwatchUiModel>,
    roundPhase: RoundPhase,
    roundKey: Int,
    onSwatchTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One Animatable per petal — scaleY driven by unfold / fold phases.
    val petalScales = remember { List(PETAL_COUNT) { Animatable(0f) } }

    // ── Unfold: triggered every time `roundKey` changes (new round) ───────────
    LaunchedEffect(roundKey) {
        // Snap all petals to zero first so a new round always starts fully closed.
        petalScales.forEach { it.snapTo(0f) }
        petalScales.forEachIndexed { idx, anim ->
            launch {
                kotlinx.coroutines.delay(idx * STAGGER_UNFOLD_MS)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    // ── Fold: triggered when ViewModel enters FoldingOut phase ────────────────
    LaunchedEffect(roundPhase) {
        if (roundPhase == RoundPhase.FoldingOut) {
            // Fold in reverse order (last petal closes first) for an elegant closing feel.
            petalScales.forEachIndexed { idx, anim ->
                launch {
                    kotlinx.coroutines.delay((PETAL_COUNT - 1 - idx) * STAGGER_FOLD_MS)
                    anim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = FOLD_DURATION_MS),
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier.size(CONTAINER_SIZE),
    ) {
        swatches.forEachIndexed { idx, swatch ->
            val angleDeg = idx * 60f
            val petalScale = petalScales[idx].value

            PetalTile(
                color = swatch.color,
                displayState = swatch.displayState,
                angleDeg = angleDeg,
                scaleY = petalScale,
                enabled = roundPhase == RoundPhase.Idle && swatch.displayState == SwatchDisplayState.Default,
                onClick = { onSwatchTap(idx) },
            )
        }
    }
}

// ── Single petal tile ─────────────────────────────────────────────────────────

@Composable
private fun PetalTile(
    color: Color,
    displayState: SwatchDisplayState,
    angleDeg: Float,
    scaleY: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // ── Shake for wrong state ─────────────────────────────────────────────────
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(displayState) {
        if (displayState == SwatchDisplayState.Wrong) {
            repeat(3) { i ->
                shakeX.animateTo(
                    targetValue = if (i % 2 == 0) -10f else 10f,
                    animationSpec = tween(durationMillis = 50),
                )
            }
            shakeX.animateTo(0f, tween(80))
        }
    }

    // ── Border colour ─────────────────────────────────────────────────────────
    val borderColor by animateColorAsState(
        targetValue = when (displayState) {
            SwatchDisplayState.Correct -> HuezooColors.AccentGreen
            SwatchDisplayState.Wrong -> HuezooColors.AccentMagenta
            SwatchDisplayState.Revealed -> HuezooColors.AccentCyan
            SwatchDisplayState.Default -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "petalBorder",
    )

    val pressScale = if (isPressed) 0.94f else 1f

    Box(
        modifier = modifier
            // Position: tip of petal lands exactly at container centre.
            // Container is CONTAINER_SIZE square; petal top-centre → container centre requires:
            //   x offset = (CONTAINER_SIZE - PETAL_WIDTH) / 2
            //   y offset = CONTAINER_SIZE / 2   (top of petal at centre Y)
            .absoluteOffset(
                x = (CONTAINER_SIZE - PETAL_WIDTH) / 2,
                y = CONTAINER_SIZE / 2,
            )
            .size(PETAL_WIDTH, PETAL_HEIGHT)
            .graphicsLayer {
                // Pivot at the tip (top-centre of the petal bounding box).
                transformOrigin = TransformOrigin(0.5f, 0f)
                rotationZ = angleDeg
                this.scaleY = scaleY * pressScale
                scaleX = pressScale
                translationX = shakeX.value
                // Allow neon glow to render outside the layer bounds for Correct + Revealed.
                clip = displayState == SwatchDisplayState.Default ||
                    displayState == SwatchDisplayState.Wrong
            }
            // Neon path-accurate glow for Correct / Revealed states.
            .drawWithContent {
                drawContent()
                val bc = borderColor
                if (bc != Color.Transparent) {
                    val outerExtra = NEON_OUTER_EXTRA_PX
                    val innerExtra = NEON_INNER_EXTRA_PX
                    val outline = SwatchPetal.createOutline(this.size, layoutDirection, this)
                    if (outline is Outline.Generic) {
                        // Outer neon ring (wider, translucent)
                        drawPath(
                            path = outline.path,
                            color = bc.copy(alpha = NEON_OUTER_ALPHA),
                            style = Stroke(width = outerExtra * 2),
                        )
                        // Inner neon ring (narrow, opaque)
                        drawPath(
                            path = outline.path,
                            color = bc,
                            style = Stroke(width = BORDER_STROKE_DP),
                        )
                    }
                }
            }
            .background(color, SwatchPetal)
            .clip(SwatchPetal)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        SwatchGradientOverlay()
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun FlowerSwatchLayoutIdlePreview() {
    HuezooPreviewTheme {
        FlowerSwatchLayout(
            swatches = listOf(
                SwatchUiModel(HuezooColors.AccentPurple),
                SwatchUiModel(HuezooColors.AccentPurple),
                SwatchUiModel(HuezooColors.AccentCyan), // odd one out
                SwatchUiModel(HuezooColors.AccentPurple),
                SwatchUiModel(HuezooColors.AccentPurple),
                SwatchUiModel(HuezooColors.AccentPurple),
            ),
            roundPhase = RoundPhase.Idle,
            roundKey = 1,
            onSwatchTap = {},
        )
    }
}

@PreviewComponent
@Composable
private fun FlowerSwatchLayoutCorrectPreview() {
    HuezooPreviewTheme {
        FlowerSwatchLayout(
            swatches = listOf(
                SwatchUiModel(HuezooColors.AccentPurple),
                SwatchUiModel(HuezooColors.AccentPurple),
                SwatchUiModel(HuezooColors.AccentCyan, displayState = SwatchDisplayState.Correct),
                SwatchUiModel(HuezooColors.AccentPurple),
                SwatchUiModel(HuezooColors.AccentPurple),
                SwatchUiModel(HuezooColors.AccentPurple),
            ),
            roundPhase = RoundPhase.Correct,
            roundKey = 1,
            onSwatchTap = {},
        )
    }
}

