package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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

@Suppress("CyclomaticComplexMethod", "LongMethod")
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

    // ── Celebrate pop + glass shatter on correct (UX.8.1) ───────────────────
    // 1. Pop    → spring to 1.18× (satisfying bloom)
    // 2. Settle → spring back to 1× (natural rebound)
    // 3. Shatter → tile hides; random parallelogram shards fall + spin + fade
    val celebrateScale = remember { Animatable(1f) }
    var isShattered by remember { mutableStateOf(false) }
    var shardColor by remember { mutableStateOf(Color.Transparent) }
    val shatterProgress = remember { Animatable(0f) }
    var currentShards by remember { mutableStateOf(emptyList<GlassShard>()) }

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
                // 3. Shatter — fresh random shards every time
                shardColor = color
                currentShards = generateShards(Random.Default)
                isShattered = true
                shatterProgress.snapTo(0f)
                shatterProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = SHATTER_DURATION_MS, easing = LinearEasing),
                )
            }
            else -> {
                isShattered = false
                currentShards = emptyList()
                shatterProgress.snapTo(0f)
                celebrateScale.snapTo(1f)
            }
        }
    }

    // ── Border colour driven by display state ─────────────────────────────────
    val levelAccent = LocalPlayerAccentColor.current
    val borderTarget = when (displayState) {
        SwatchDisplayState.Correct -> levelAccent
        SwatchDisplayState.Wrong -> Color.White
        SwatchDisplayState.Revealed -> levelAccent
        SwatchDisplayState.Default -> Color.Transparent
    }
    val borderColor by animateColorAsState(
        targetValue = borderTarget,
        animationSpec = if (borderTarget == Color.Transparent) tween(0) else tween(200),
        label = "tileBorder",
    )

    val pressScale = if (isPressed) 0.94f else 1f

    // Pivot fraction in tile-local Y: the flower centre is centerGap ABOVE the tile's top edge.
    val pivotY = -(config.centerGap.value / config.tileHeight.value)

    // ── Outer Box: positioning only ───────────────────────────────────────────
    Box(
        modifier = modifier
            .absoluteOffset(
                x = (config.containerSize - config.tileWidth) / 2,
                y = config.containerSize / 2 + config.centerGap,
            )
            .size(config.tileWidth, config.tileHeight),
    ) {
        // ── Main tile ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, pivotY)
                    rotationZ = angleDeg
                    val s = tileScale * pressScale * celebrateScale.value
                    if (config.uniformScale) {
                        scaleX = s
                        scaleY = s
                    } else {
                        scaleX = pressScale * celebrateScale.value
                        scaleY = s
                    }
                    translationX = shakeX.value
                    clip = displayState == SwatchDisplayState.Default || displayState == SwatchDisplayState.Wrong
                    // Hide tile body while shards are flying — shatter Canvas takes over visually.
                    alpha = if (isShattered) 0f else 1f
                }
                .drawWithContent {
                    drawContent()
                    val bc = borderColor
                    if (bc != Color.Transparent) {
                        when (val outline = shape.createOutline(this.size, layoutDirection, this)) {
                            is Outline.Generic -> {
                                drawPath(
                                    path = outline.path,
                                    color = bc.copy(alpha = NEON_OUTER_ALPHA),
                                    style = Stroke(width = NEON_OUTER_STROKE_PX),
                                )
                                drawPath(path = outline.path, color = bc, style = Stroke(width = NEON_INNER_STROKE_PX))
                            }
                            is Outline.Rounded -> drawRoundRect(
                                color = bc,
                                cornerRadius = outline.roundRect.topLeftCornerRadius,
                                style = Stroke(width = NEON_INNER_STROKE_PX),
                            )
                            is Outline.Rectangle -> drawRect(color = bc, style = Stroke(width = NEON_INNER_STROKE_PX))
                        }
                    }
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

        // ── Glass shatter overlay ─────────────────────────────────────────────
        // Rendered at the same rotation as the tile but with clip = false so shards
        // can fly beyond the tile bounds. Reads shatterProgress.value in composable
        // scope so recomposition drives each animation frame.
        val t = shatterProgress.value
        if (t > 0f) {
            val capturedShardColor = shardColor
            val capturedShards = currentShards
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, pivotY)
                        rotationZ = angleDeg
                        clip = false
                    },
            ) {
                // Gravity fall: pieces accelerate downward (t² for quadratic curve)
                val gravityT = t * t
                // Time-based fade: start dissolving at t=0.5, fully gone at t=1.0
                val timeFade = (1f - ((t - 0.5f) / 0.5f).coerceIn(0f, 1f))

                // Screen-space fall direction in canvas-local coords.
                // The canvas is rotated by angleDeg, so to fall screen-down we
                // apply the inverse rotation: canvas_down = (sin θ, cos θ).
                val angleRad = angleDeg * (PI / 180.0).toFloat()
                val sinA = sin(angleRad)
                val cosA = cos(angleRad)
                val fallMag = size.height * SHARD_FALL_DISTANCE * gravityT

                capturedShards.forEach { shard ->
                    val driftMag = shard.driftX * size.width * SHARD_DRIFT * t
                    // Canvas-local displacement → screen-space down + lateral drift
                    val dx = sinA * fallMag + cosA * driftMag
                    val dy = cosA * fallMag - sinA * driftMag
                    val spin = shard.spinDir * SHARD_SPIN_DEGREES * t

                    val centroidPx = Offset(
                        shard.centroid.x * size.width + dx,
                        shard.centroid.y * size.height + dy,
                    )

                    // Parallelogram quad path — 4 vertices
                    val path = Path().apply {
                        moveTo(shard.p0.x * size.width + dx, shard.p0.y * size.height + dy)
                        lineTo(shard.p1.x * size.width + dx, shard.p1.y * size.height + dy)
                        lineTo(shard.p2.x * size.width + dx, shard.p2.y * size.height + dy)
                        lineTo(shard.p3.x * size.width + dx, shard.p3.y * size.height + dy)
                        close()
                    }

                    withTransform({ rotate(spin, centroidPx) }) {
                        drawPath(path, color = capturedShardColor, alpha = timeFade)
                        // Glass-edge shimmer — thin white stroke along each shard edge
                        drawPath(
                            path,
                            color = Color.White,
                            alpha = timeFade * SHARD_SHIMMER_ALPHA,
                            style = Stroke(width = SHARD_EDGE_STROKE_PX),
                        )
                    }
                }
            }
        }
    }
}

// ── Glass shatter data ────────────────────────────────────────────────────────

// Parallelogram-like quad shard in normalized 0..1 tile space.
// p0=top-left, p1=top-right, p2=bottom-right, p3=bottom-left (roughly).
private data class GlassShard(
    val p0: Offset,
    val p1: Offset,
    val p2: Offset,
    val p3: Offset,
    val centroid: Offset,
    val spinDir: Float, // +1 or -1 — adjacent shards spin opposite ways
    val driftX: Float, // -1..1 horizontal drift multiplier
)

// Generates a randomized grid of parallelogram quads in normalized 0..1 space.
// Called fresh per shatter so every break looks different — count, size, and lean
// all vary. Inner vertices are jittered and a global skew makes every quad
// look like a true parallelogram rather than a rectangle.
@Suppress("MagicNumber")
private fun generateShards(rng: Random): List<GlassShard> {
    val cols = 2 + rng.nextInt(3) // 2..4 columns
    val rows = 3 + rng.nextInt(3) // 3..5 rows

    // Build jittered grid: edge vertices stay on boundary; interior vertices shift.
    val verts = Array(rows + 1) { r ->
        Array(cols + 1) { c ->
            val bx = c.toFloat() / cols
            val by = r.toFloat() / rows
            val jx = if (c in 1 until cols) (rng.nextFloat() * 2 - 1) * 0.18f / cols else 0f
            val jy = if (r in 1 until rows) (rng.nextFloat() * 2 - 1) * 0.18f / rows else 0f
            Offset((bx + jx).coerceIn(0f, 1f), (by + jy).coerceIn(0f, 1f))
        }
    }
    // Global parallelogram skew: shift each row's inner vertices in X so all quads
    // share the same lean — like a glass sheet sheared before it cracked.
    val skew = (rng.nextFloat() * 2 - 1) * 0.06f
    for (r in 1 until rows) {
        for (c in 1 until cols) {
            verts[r][c] = Offset(
                (verts[r][c].x + skew * r).coerceIn(0.02f, 0.98f),
                verts[r][c].y,
            )
        }
    }
    return buildList {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val tl = verts[r][c]
                val tr = verts[r][c + 1]
                val br = verts[r + 1][c + 1]
                val bl = verts[r + 1][c]
                val cx = (tl.x + tr.x + br.x + bl.x) / 4f
                val cy = (tl.y + tr.y + br.y + bl.y) / 4f
                val spinDir = if ((r + c) % 2 == 0) 1f else -1f
                val driftX = (rng.nextFloat() * 2f - 1f) * 0.9f
                add(GlassShard(tl, tr, br, bl, Offset(cx, cy), spinDir, driftX))
            }
        }
    }
}

private const val SHATTER_DURATION_MS = 600
private const val SHARD_FALL_DISTANCE = 1.6f // tile-height multiples to fall under gravity
private const val SHARD_DRIFT = 0.35f // horizontal drift as fraction of tile width
private const val SHARD_SPIN_DEGREES = 70f
private const val SHARD_SHIMMER_ALPHA = 0.35f
private const val SHARD_EDGE_STROKE_PX = 1.5f

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
