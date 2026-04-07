package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import xyz.ksharma.huezoo.ui.model.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.model.SwatchLayoutStyle
import xyz.ksharma.huezoo.ui.model.SwatchSize
import xyz.ksharma.huezoo.ui.model.SwatchUiModel
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.CitrusSwatch
import xyz.ksharma.huezoo.ui.theme.DiamondSwatch
import xyz.ksharma.huezoo.ui.theme.HexagonSwatch
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.ShieldSwatch
import xyz.ksharma.huezoo.ui.theme.SquircleMedium
import xyz.ksharma.huezoo.ui.theme.SquircleSmall
import xyz.ksharma.huezoo.ui.theme.SwatchPetal

// ── Active swatch size ────────────────────────────────────────────────────────

/**
 * Swap between [SwatchSize.Normal] and [SwatchSize.Medium] here to toggle tile size.
 * Users always see whichever value is set here — this is not exposed to the UI layer.
 */
internal val ACTIVE_SWATCH_SIZE = SwatchSize.Medium

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

/**
 * Returns the layout geometry for [style] at the given [size].
 *
 * Container stays fixed at [SHARED_CONTAINER] = 300 dp so the layout never overflows its
 * parent. For [SwatchSize.Medium] (1.2×) the tile dimensions grow; [centerGap] is trimmed
 * only where needed (Flower, SpokeBlades) to keep outer tile tips inside the container.
 *
 * Derivation for overflow safety:
 *   max tile reach = containerSize/2 + centerGap + tileHeight ≤ containerSize
 *   → centerGap ≤ containerSize/2 − tileHeight
 */
private fun configFor(style: SwatchLayoutStyle, size: SwatchSize): RadialConfig = when (style) {
    SwatchLayoutStyle.Flower -> when (size) {
        SwatchSize.Normal -> RadialConfig(84.dp, 116.dp, SHARED_CONTAINER, centerGap = 18.dp)
        SwatchSize.Medium -> RadialConfig(101.dp, 139.dp, SHARED_CONTAINER, centerGap = 10.dp)
    }
    SwatchLayoutStyle.HexRing -> when (size) {
        SwatchSize.Normal -> RadialConfig(68.dp, 68.dp, SHARED_CONTAINER, centerGap = 24.dp, uniformScale = true)
        SwatchSize.Medium -> RadialConfig(82.dp, 82.dp, SHARED_CONTAINER, centerGap = 24.dp, uniformScale = true)
    }
    SwatchLayoutStyle.SquircleOrbit -> when (size) {
        SwatchSize.Normal -> RadialConfig(66.dp, 66.dp, SHARED_CONTAINER, centerGap = 28.dp, uniformScale = true)
        SwatchSize.Medium -> RadialConfig(80.dp, 80.dp, SHARED_CONTAINER, centerGap = 24.dp, uniformScale = true)
    }
    SwatchLayoutStyle.SpokeBlades -> when (size) {
        SwatchSize.Normal -> RadialConfig(42.dp, 106.dp, SHARED_CONTAINER, centerGap = 20.dp)
        SwatchSize.Medium -> RadialConfig(50.dp, 120.dp, SHARED_CONTAINER, centerGap = 14.dp)
    }
    SwatchLayoutStyle.DiamondHalo -> when (size) {
        SwatchSize.Normal -> RadialConfig(62.dp, 62.dp, SHARED_CONTAINER, centerGap = 24.dp)
        SwatchSize.Medium -> RadialConfig(74.dp, 74.dp, SHARED_CONTAINER, centerGap = 24.dp)
    }
    SwatchLayoutStyle.ShieldRing -> when (size) {
        // Width reduced (86→72, 103→86) to give the kite-diamond a ~0.67 aspect ratio —
        // elongated and visually distinct from the square DiamondHalo layout.
        SwatchSize.Normal -> RadialConfig(72.dp, 108.dp, SHARED_CONTAINER, centerGap = 36.dp)
        SwatchSize.Medium -> RadialConfig(86.dp, 130.dp, SHARED_CONTAINER, centerGap = 18.dp)
    }
    SwatchLayoutStyle.CitrusSlice -> when (size) {
        // Landscape tile — wider than tall; uniformScale so the slice pops in as a whole.
        SwatchSize.Normal -> RadialConfig(88.dp, 52.dp, SHARED_CONTAINER, centerGap = 30.dp, uniformScale = true)
        SwatchSize.Medium -> RadialConfig(106.dp, 62.dp, SHARED_CONTAINER, centerGap = 34.dp, uniformScale = true)
    }
}

private fun shapeFor(style: SwatchLayoutStyle): Shape = when (style) {
    SwatchLayoutStyle.Flower -> SwatchPetal
    SwatchLayoutStyle.HexRing -> HexagonSwatch
    SwatchLayoutStyle.SquircleOrbit -> SquircleMedium
    SwatchLayoutStyle.SpokeBlades -> SquircleSmall
    SwatchLayoutStyle.DiamondHalo -> DiamondSwatch
    SwatchLayoutStyle.ShieldRing -> ShieldSwatch
    SwatchLayoutStyle.CitrusSlice -> CitrusSwatch
}

// ── Animation constants ───────────────────────────────────────────────────────

private const val TILE_COUNT = 6
private const val STAGGER_UNFOLD_MS = 70L // delay between successive tile openings
private const val STAGGER_FOLD_MS = 45L // delay between successive tile closings (faster)
private const val FOLD_DURATION_MS = 210 // duration per tile fold animation
private const val NEON_OUTER_STROKE_PX = 12f
private const val NEON_INNER_STROKE_PX = 6f
private const val NEON_OUTER_ALPHA = 0.30f

// DEBUG ONLY — debug odd-swatch border constants. Only used when SwatchUiModel.isDebugOdd = true.
private const val DEBUG_ODD_STROKE_PX = 4f
private const val DEBUG_ODD_ALPHA = 0.55f

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
 * @param layoutStyle     Which of the built-in shapes and geometries to use.
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
    val config = remember(layoutStyle) { configFor(layoutStyle, ACTIVE_SWATCH_SIZE) }
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
                isDebugOdd = swatch.isDebugOdd,
            )
        }
    }
}

// ── Single tile ───────────────────────────────────────────────────────────────

@Suppress("CyclomaticComplexMethod")
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
    // DEBUG ONLY — always false in release builds. Renders a subtle white border
    // on this tile so manual testers can identify the correct answer without guessing.
    isDebugOdd: Boolean = false,
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

    // ── Celebrate pop + implode on correct (UX.8.1) ──────────────────────────
    // Three-step animation:
    //   1. Pop    → spring to 1.18× (satisfying bloom)
    //   2. Settle → spring back toward 1× (natural rebound)
    //   3. Implode → fast spring collapse to 0 (swatch vanishes before FoldingOut)
    // Total time: ~250 ms (pop+settle) + ~180 ms (implode) ≈ 430 ms, well within
    // the 750 ms ANIMATION_CORRECT_MS window — nothing feels rushed or clipped.
    val celebrateScale = remember { Animatable(1f) }
    LaunchedEffect(displayState) {
        when (displayState) {
            SwatchDisplayState.Correct -> {
                // 1. Pop
                celebrateScale.animateTo(
                    targetValue = 1.18f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessHigh,
                    ),
                )
                // 2. Settle
                celebrateScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
                // 3. Implode — correct swatch shrinks to nothing before the next round unfolds
                celebrateScale.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessHigh,
                    ),
                )
            }
            else -> celebrateScale.snapTo(1f)
        }
    }

    // ── Border colour driven by display state ─────────────────────────────────
    // Correct / Revealed → player's level accent (cyan at Rookie, green at Trained, etc.)
    // Wrong              → pure white — neutral "you tapped this one" indicator
    val levelAccent = LocalPlayerAccentColor.current
    val borderColor by animateColorAsState(
        targetValue = when (displayState) {
            SwatchDisplayState.Correct -> levelAccent
            SwatchDisplayState.Wrong -> Color.White
            SwatchDisplayState.Revealed -> levelAccent
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
                val s = tileScale * pressScale * celebrateScale.value
                if (config.uniformScale) {
                    scaleX = s
                    scaleY = s
                } else {
                    scaleX = pressScale * celebrateScale.value // tangential: press + celebrate bloom
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

                // DEBUG ONLY: dim white border that marks the odd (correct) tile.
                // Visible only when isDebugOdd = true, which the ViewModel sets exclusively
                // in debug builds (PlatformOps.isDebugBuild). Never rendered in release.
                if (isDebugOdd && displayState == SwatchDisplayState.Default) {
                    when (val outline = shape.createOutline(this.size, layoutDirection, this)) {
                        is Outline.Generic -> drawPath(
                            path = outline.path,
                            color = Color.White.copy(alpha = DEBUG_ODD_ALPHA),
                            style = Stroke(width = DEBUG_ODD_STROKE_PX),
                        )
                        is Outline.Rounded -> drawRoundRect(
                            color = Color.White.copy(alpha = DEBUG_ODD_ALPHA),
                            cornerRadius = outline.roundRect.topLeftCornerRadius,
                            style = Stroke(width = DEBUG_ODD_STROKE_PX),
                        )
                        is Outline.Rectangle -> drawRect(
                            color = Color.White.copy(alpha = DEBUG_ODD_ALPHA),
                            style = Stroke(width = DEBUG_ODD_STROKE_PX),
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

// ── Swatch-shape progress ring ────────────────────────────────────────────────

/**
 * Six ghost outlines — one per tile — positioned identically to [RadialSwatchLayout] but scaled
 * slightly outward so they glow around the real swatches.
 *
 * Place this *behind* [RadialSwatchLayout] in a stacked [Box].  Each ghost fills with the
 * player's accent colour as the consecutive-correct-tap streak climbs through six milestones.
 * Resets (drains) smoothly when [correctStreak] drops back to 0 (wrong tap / new try).
 *
 * Milestones: [1, 3, 6, 10, 15, 21] correct taps to fill each ghost fully.
 */
@Composable
fun SwatchShapeProgressRing(
    layoutStyle: SwatchLayoutStyle,
    correctStreak: Int,
    roundPhase: RoundPhase,
    modifier: Modifier = Modifier,
) {
    val config = remember(layoutStyle) { configFor(layoutStyle, ACTIVE_SWATCH_SIZE) }
    val shape = remember(layoutStyle) { shapeFor(layoutStyle) }
    val accent = LocalPlayerAccentColor.current

    Box(modifier = modifier.size(config.containerSize)) {
        repeat(TILE_COUNT) { idx ->
            val angleDeg = idx * (360f / TILE_COUNT)
            val milestone = SWATCH_PROGRESS_MILESTONES[idx]
            val prevMilestone = if (idx == 0) 0 else SWATCH_PROGRESS_MILESTONES[idx - 1]
            val targetFill = when {
                correctStreak >= milestone -> 1f
                correctStreak <= prevMilestone -> 0f
                else -> (correctStreak - prevMilestone).toFloat() / (milestone - prevMilestone)
            }
            val animatedFill by animateFloatAsState(
                targetValue = targetFill,
                animationSpec = tween(if (roundPhase == RoundPhase.Wrong) 420 else 230),
                label = "ghostFill_$idx",
            )

            val pivotY = -(config.centerGap.value / config.tileHeight.value)

            Canvas(
                modifier = Modifier
                    .absoluteOffset(
                        x = (config.containerSize - config.tileWidth) / 2,
                        y = config.containerSize / 2 + config.centerGap,
                    )
                    .size(config.tileWidth, config.tileHeight)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, pivotY)
                        rotationZ = angleDeg
                        // Scale outward so the ghost peeks around the real tile edges.
                        if (config.uniformScale) {
                            scaleX = GHOST_SCALE_UNIFORM
                            scaleY = GHOST_SCALE_UNIFORM
                        } else {
                            scaleX = GHOST_SCALE_TANGENTIAL
                            scaleY = GHOST_SCALE_RADIAL
                        }
                        clip = false
                    },
            ) {
                val outline = shape.createOutline(size, layoutDirection, this)
                if (outline is Outline.Generic) {
                    // Filled body — grows from transparent to solid as the ghost fills
                    if (animatedFill > 0f) {
                        drawPath(
                            path = outline.path,
                            color = accent.copy(alpha = animatedFill * GHOST_FILL_ALPHA),
                        )
                    }
                    // Always-visible dim stroke — brightens to neon as the ghost fills
                    drawPath(
                        path = outline.path,
                        color = accent.copy(alpha = GHOST_TRACK_ALPHA + animatedFill * GHOST_GLOW_ALPHA),
                        style = Stroke(width = GHOST_STROKE_PX),
                    )
                }
            }
        }
    }
}

private val SWATCH_PROGRESS_MILESTONES = intArrayOf(1, 3, 6, 10, 15, 21)

private const val GHOST_SCALE_UNIFORM = 1.10f // uniform shapes (hex, squircle, diamond)
private const val GHOST_SCALE_RADIAL = 1.12f // petal/blade: grow along radial axis
private const val GHOST_SCALE_TANGENTIAL = 1.04f // petal/blade: subtle tangential spread
private const val GHOST_STROKE_PX = 3.5f
private const val GHOST_TRACK_ALPHA = 0.14f // always-visible dim track
private const val GHOST_FILL_ALPHA = 0.38f // max fill body alpha
private const val GHOST_GLOW_ALPHA = 0.62f // additional stroke alpha when fully lit

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
private fun AllSwatchStylesGalleryPreview() {
    // All remaining styles in a 2-column grid, each scaled to 50 % so the full ring is visible.
    // Use this as the single source of truth when evaluating shape/size changes.
    val styles = SwatchLayoutStyle.entries
    HuezooPreviewTheme {
        Column {
            styles.chunked(2).forEach { rowStyles ->
                Row {
                    rowStyles.forEach { style ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Scale the 300 dp container to 150 dp so two fit side-by-side.
                            Box(
                                modifier = Modifier.size(150.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(SHARED_CONTAINER)
                                        .graphicsLayer {
                                            scaleX = 0.5f
                                            scaleY = 0.5f
                                        },
                                ) {
                                    RadialSwatchLayout(
                                        swatches = previewSwatches(oddIndex = 2),
                                        roundPhase = RoundPhase.Idle,
                                        roundKey = 1,
                                        layoutStyle = style,
                                        onSwatchTap = {},
                                    )
                                }
                            }
                            HuezooLabelSmall(
                                text = style.name.uppercase(),
                                color = HuezooColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewComponent
@Composable
private fun RadialShieldRingPreview() {
    HuezooPreviewTheme {
        RadialSwatchLayout(
            swatches = previewSwatches(oddIndex = 2),
            roundPhase = RoundPhase.Idle,
            roundKey = 1,
            layoutStyle = SwatchLayoutStyle.ShieldRing,
            onSwatchTap = {},
        )
    }
}

@PreviewComponent
@Composable
private fun RadialCitrusSlicePreview() {
    HuezooPreviewTheme {
        RadialSwatchLayout(
            swatches = previewSwatches(oddIndex = 3),
            roundPhase = RoundPhase.Idle,
            roundKey = 1,
            layoutStyle = SwatchLayoutStyle.CitrusSlice,
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
