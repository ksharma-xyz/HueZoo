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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.games.threshold.state.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.model.SwatchLayoutStyle
import xyz.ksharma.huezoo.ui.model.SwatchUiModel
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.DiamondSwatch
import xyz.ksharma.huezoo.ui.theme.HexagonSwatch
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.SquircleMedium
import xyz.ksharma.huezoo.ui.theme.SquircleSmall
import xyz.ksharma.huezoo.ui.theme.SwatchPetal

// ── Layout configs ────────────────────────────────────────────────────────────

/**
 * Per-style geometry that drives tile placement and animation.
 *
 * @param tileWidth  Tangential extent of each tile (left-right in its local frame).
 * @param tileHeight Radial extent of each tile (the dimension that grows outward from centre).
 * @param containerSize Square bounding box that contains all tiles.
 * @param centerGap Distance from the flower's centre point to each tile's inner edge (top of
 *   tile bounding box).  Creates visible breathing room between tiles and the centre.
 * @param uniformScale When `true`, both scaleX and scaleY are animated together (good for
 *   symmetric shapes like squircles/hexagons — tiles pop out as a whole).  When `false`, only
 *   scaleY is animated (tiles grow/retract along their radial axis — good for petals/spokes).
 */
private data class RadialConfig(
    val tileWidth: Dp,
    val tileHeight: Dp,
    val containerSize: Dp,
    val centerGap: Dp,
    val uniformScale: Boolean = false,
)

private val SHARED_CONTAINER = 300.dp

private fun configFor(style: SwatchLayoutStyle): RadialConfig = when (style) {
    // Petals taper toward centre — slight tangential overlap is intentional (natural flower feel).
    SwatchLayoutStyle.Flower -> RadialConfig(
        tileWidth = 84.dp, tileHeight = 116.dp,
        containerSize = SHARED_CONTAINER, centerGap = 18.dp,
        uniformScale = false,
    )
    // Hexagons: uniform scale so they pop out symmetrically.
    // 60 dp tiles at 28 dp gap → tangential slot ≈ 63 dp — tiles just clear each other.
    SwatchLayoutStyle.HexRing -> RadialConfig(
        tileWidth = 60.dp, tileHeight = 60.dp,
        containerSize = SHARED_CONTAINER, centerGap = 28.dp,
        uniformScale = true,
    )
    // Squircles: uniform scale, small gap for a clean orbit feel.
    // 58 dp tiles at 32 dp gap → tangential slot ≈ 64 dp — clear spacing.
    SwatchLayoutStyle.SquircleOrbit -> RadialConfig(
        tileWidth = 58.dp, tileHeight = 58.dp,
        containerSize = SHARED_CONTAINER, centerGap = 32.dp,
        uniformScale = true,
    )
    // Spokes are narrow (36 dp) so there's generous air between them at any radius.
    SwatchLayoutStyle.SpokeBlades -> RadialConfig(
        tileWidth = 36.dp, tileHeight = 92.dp,
        containerSize = SHARED_CONTAINER, centerGap = 24.dp,
        uniformScale = false,
    )
    // Diamonds at 54 dp with 28 dp gap → tangential slot ≈ 58 dp — clear spacing.
    SwatchLayoutStyle.DiamondHalo -> RadialConfig(
        tileWidth = 54.dp, tileHeight = 54.dp,
        containerSize = SHARED_CONTAINER, centerGap = 28.dp,
        uniformScale = false,
    )
}

private fun shapeFor(style: SwatchLayoutStyle): Shape = when (style) {
    SwatchLayoutStyle.Flower -> SwatchPetal
    SwatchLayoutStyle.HexRing -> HexagonSwatch
    SwatchLayoutStyle.SquircleOrbit -> SquircleMedium
    SwatchLayoutStyle.SpokeBlades -> SquircleSmall
    SwatchLayoutStyle.DiamondHalo -> DiamondSwatch
}

// ── Animation constants ───────────────────────────────────────────────────────

private const val TILE_COUNT = 6
private const val STAGGER_UNFOLD_MS = 70L   // delay between successive tile openings
private const val STAGGER_FOLD_MS = 45L     // delay between successive tile closings (faster)
private const val FOLD_DURATION_MS = 210    // duration per tile fold animation
private const val NEON_OUTER_STROKE_PX = 8f
private const val NEON_INNER_STROKE_PX = 4f
private const val NEON_OUTER_ALPHA = 0.30f

// ── Public composable ─────────────────────────────────────────────────────────

/**
 * Reusable radial swatch picker — 6 coloured tiles arranged in a ring around a hidden centre.
 *
 * Drop-in anywhere you need an "odd-colour-out" puzzle.  Swap [layoutStyle] to change the
 * tile shape and geometry with zero other code changes.
 *
 * ## Layout geometry
 * Every style uses the same coordinate model:
 * ```
 *            centre
 *              │← centerGap →│← tileHeight →│
 *              ╳             ┌──────────┐
 *                            │   tile   │   (at angle 0°, pointing straight down)
 *                            └──────────┘
 * ```
 * The tile's top edge (inner tip / flat edge) is `centerGap` away from the layout centre.
 * Each tile is then rotated by `index × 60°` **around the layout centre** (not around the
 * tile's own top edge), so the gap is preserved for every petal angle.
 *
 * ## Animation sequence
 * | Trigger | Effect |
 * |---------|--------|
 * | `roundKey` changes | All tiles snap to 0, then stagger-unfold (scale 0 → 1 from centre outward) |
 * | `roundPhase == FoldingOut` | All tiles stagger-fold in reverse order (scale 1 → 0) |
 * | `SwatchDisplayState.Correct` | Green neon path border on that tile |
 * | `SwatchDisplayState.Wrong` | Magenta neon border + horizontal shake |
 * | `SwatchDisplayState.Revealed` | Cyan neon border |
 *
 * ## Reuse on other screens
 * Pass any [SwatchLayoutStyle] and supply your own `onSwatchTap` handler.  The composable has
 * no knowledge of game logic; it only renders colours and emits tap indices.
 *
 * @param swatches        Exactly [TILE_COUNT] (6) swatch models. Extra entries are ignored;
 *                        fewer entries render fewer tiles.
 * @param roundPhase      Current animation phase — controls unfold / fold transitions and
 *                        whether taps are accepted.
 * @param roundKey        Changes every time a new round starts.  Triggers the reset + unfold.
 * @param layoutStyle     Which of the 5 built-in shapes and geometries to use.
 * @param onSwatchTap     Called with the 0-based index of the tapped tile.
 */
@Composable
fun RadialSwatchLayout(
    swatches: List<SwatchUiModel>,
    roundPhase: RoundPhase,
    roundKey: Int,
    layoutStyle: SwatchLayoutStyle,
    onSwatchTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = remember(layoutStyle) { configFor(layoutStyle) }
    val shape = remember(layoutStyle) { shapeFor(layoutStyle) }

    // One Animatable (0 → 1) per tile, shared across layout-style changes.
    val tileScales = remember { List(TILE_COUNT) { Animatable(0f) } }

    // ── Unfold: every new round ───────────────────────────────────────────────
    LaunchedEffect(roundKey) {
        tileScales.forEach { it.snapTo(0f) }
        tileScales.forEachIndexed { idx, anim ->
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

    // ── Fold: when ViewModel transitions into FoldingOut ─────────────────────
    LaunchedEffect(roundPhase) {
        if (roundPhase == RoundPhase.FoldingOut) {
            tileScales.forEachIndexed { idx, anim ->
                launch {
                    // Reverse order: last tile starts folding first → elegant closing motion.
                    kotlinx.coroutines.delay((TILE_COUNT - 1 - idx) * STAGGER_FOLD_MS)
                    anim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = FOLD_DURATION_MS),
                    )
                }
            }
        }
    }

    Box(modifier = modifier.size(config.containerSize)) {
        swatches.take(TILE_COUNT).forEachIndexed { idx, swatch ->
            val angleDeg = idx * (360f / TILE_COUNT)
            RadialTile(
                color = swatch.color,
                displayState = swatch.displayState,
                angleDeg = angleDeg,
                tileScale = tileScales[idx].value,
                config = config,
                shape = shape,
                enabled = roundPhase == RoundPhase.Idle &&
                    swatch.displayState == SwatchDisplayState.Default,
                onClick = { onSwatchTap(idx) },
            )
        }
    }
}

// ── Single tile ───────────────────────────────────────────────────────────────

@Composable
private fun RadialTile(
    color: Color,
    displayState: SwatchDisplayState,
    angleDeg: Float,
    tileScale: Float,
    config: RadialConfig,
    shape: Shape,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // ── Shake for wrong ───────────────────────────────────────────────────────
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(displayState) {
        if (displayState == SwatchDisplayState.Wrong) {
            repeat(3) { i ->
                shakeX.animateTo(if (i % 2 == 0) -10f else 10f, tween(50))
            }
            shakeX.animateTo(0f, tween(80))
        }
    }

    // ── Border colour driven by display state ─────────────────────────────────
    val borderColor by animateColorAsState(
        targetValue = when (displayState) {
            SwatchDisplayState.Correct -> HuezooColors.AccentGreen
            SwatchDisplayState.Wrong -> HuezooColors.AccentMagenta
            SwatchDisplayState.Revealed -> HuezooColors.AccentCyan
            SwatchDisplayState.Default -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "tileBorder",
    )

    val pressScale = if (isPressed) 0.94f else 1f

    // ── Pivot at the flower CENTRE (not the tile's own top edge) ─────────────
    // The tile top-left is placed at:
    //   x = (containerSize - tileWidth) / 2         → horizontally centred on container
    //   y = containerSize / 2 + centerGap           → inner tip is centerGap BELOW container centre
    //
    // The flower centre in tile-local coordinates (origin = tile top-left) is:
    //   local_x = tileWidth / 2                     → fraction 0.5f (horizontal centre of tile)
    //   local_y = -(centerGap)                      → fraction = -centerGap / tileHeight
    //                                                  (negative = ABOVE the tile's top edge)
    //
    // Using this as TransformOrigin means ALL rotations and scales pivot around the flower
    // centre, so the gap is preserved for every rotation angle and every scale value.
    val pivotY = -(config.centerGap.value / config.tileHeight.value)

    Box(
        modifier = modifier
            // ── Position: inner tip at (centerGap) below container centre ───
            .absoluteOffset(
                x = (config.containerSize - config.tileWidth) / 2,
                y = config.containerSize / 2 + config.centerGap,
            )
            .size(config.tileWidth, config.tileHeight)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, pivotY)
                rotationZ = angleDeg

                // Uniform scale (squircle, hex): tiles pop out as a whole.
                // Directional scale (petal, spoke, diamond): tiles grow along radial axis only.
                val s = tileScale * pressScale
                if (config.uniformScale) {
                    scaleX = s
                    scaleY = s
                } else {
                    scaleX = pressScale   // only press feedback on X
                    scaleY = s
                }

                translationX = shakeX.value

                // Allow neon glow to bleed outside layer bounds for Correct / Revealed.
                clip = displayState == SwatchDisplayState.Default ||
                    displayState == SwatchDisplayState.Wrong
            }
            // ── Path-accurate neon border ─────────────────────────────────
            // Uses the same Shape path as the clip so the border always follows
            // the tile geometry exactly (works for petals, hexagons, diamonds, etc.).
            .drawWithContent {
                drawContent()
                val bc = borderColor
                if (bc != Color.Transparent) {
                    when (val outline = shape.createOutline(this.size, layoutDirection, this)) {
                        is Outline.Generic -> {
                            // Outer glow ring (wide, translucent)
                            drawPath(
                                path = outline.path,
                                color = bc.copy(alpha = NEON_OUTER_ALPHA),
                                style = Stroke(width = NEON_OUTER_STROKE_PX),
                            )
                            // Inner crisp ring (narrow, opaque)
                            drawPath(
                                path = outline.path,
                                color = bc,
                                style = Stroke(width = NEON_INNER_STROKE_PX),
                            )
                        }
                        is Outline.Rounded -> drawRoundRect(
                            color = bc,
                            cornerRadius = outline.roundRect.topLeftCornerRadius,
                            style = Stroke(width = NEON_INNER_STROKE_PX),
                        )
                        is Outline.Rectangle -> drawRect(
                            color = bc,
                            style = Stroke(width = NEON_INNER_STROKE_PX),
                        )
                    }
                }
            }
            .background(color, shape)
            .clip(shape)
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

private val PREVIEW_BASE = HuezooColors.AccentPurple
private val PREVIEW_ODD = HuezooColors.AccentCyan

private fun previewSwatches(oddIndex: Int = 2) = List(TILE_COUNT) { i ->
    SwatchUiModel(
        color = if (i == oddIndex) PREVIEW_ODD else PREVIEW_BASE,
        displayState = SwatchDisplayState.Default,
    )
}

@PreviewComponent
@Composable
private fun RadialFlowerPreview() {
    HuezooPreviewTheme {
        RadialSwatchLayout(
            swatches = previewSwatches(),
            roundPhase = RoundPhase.Idle,
            roundKey = 1,
            layoutStyle = SwatchLayoutStyle.Flower,
            onSwatchTap = {},
        )
    }
}

@PreviewComponent
@Composable
private fun RadialHexRingPreview() {
    HuezooPreviewTheme {
        RadialSwatchLayout(
            swatches = previewSwatches(oddIndex = 4),
            roundPhase = RoundPhase.Idle,
            roundKey = 1,
            layoutStyle = SwatchLayoutStyle.HexRing,
            onSwatchTap = {},
        )
    }
}

@PreviewComponent
@Composable
private fun RadialSquircleOrbitPreview() {
    HuezooPreviewTheme {
        RadialSwatchLayout(
            swatches = previewSwatches(oddIndex = 1),
            roundPhase = RoundPhase.Idle,
            roundKey = 1,
            layoutStyle = SwatchLayoutStyle.SquircleOrbit,
            onSwatchTap = {},
        )
    }
}

@PreviewComponent
@Composable
private fun RadialSpokesBladesPreview() {
    HuezooPreviewTheme {
        RadialSwatchLayout(
            swatches = previewSwatches(oddIndex = 3),
            roundPhase = RoundPhase.Idle,
            roundKey = 1,
            layoutStyle = SwatchLayoutStyle.SpokeBlades,
            onSwatchTap = {},
        )
    }
}

@PreviewComponent
@Composable
private fun RadialDiamondHaloPreview() {
    HuezooPreviewTheme {
        RadialSwatchLayout(
            swatches = previewSwatches(oddIndex = 5),
            roundPhase = RoundPhase.Idle,
            roundKey = 1,
            layoutStyle = SwatchLayoutStyle.DiamondHalo,
            onSwatchTap = {},
        )
    }
}

@PreviewComponent
@Composable
private fun RadialCorrectStatePreview() {
    HuezooPreviewTheme {
        RadialSwatchLayout(
            swatches = List(TILE_COUNT) { i ->
                SwatchUiModel(
                    color = if (i == 2) PREVIEW_ODD else PREVIEW_BASE,
                    displayState = if (i == 2) SwatchDisplayState.Correct else SwatchDisplayState.Default,
                )
            },
            roundPhase = RoundPhase.Correct,
            roundKey = 1,
            layoutStyle = SwatchLayoutStyle.Flower,
            onSwatchTap = {},
        )
    }
}

