package xyz.ksharma.huezoo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val LevelShelfOffset = 4.dp
private const val LOCKED_ALPHA = 0.38f

// ── Animation timing ───────────────────────────────────────────────────────────
private const val HEADER_TYPEWRITER_CHAR_MS = 40L   // ms per character
private const val HEADER_ACCENT_BAR_DELAY_MS = 50L  // when accent bar starts growing
private const val HEADER_DIVIDER_DELAY_MS = 120L    // when divider starts tracing
private const val SCAN_START_DELAY_MS = 180L        // neon scan line sweep start
private const val SCAN_DURATION_MS = 500            // total scan sweep duration
private const val CARD_FIRST_DELAY_MS = 300L        // first card entry
private const val CARD_STAGGER_MS = 130L            // gap between each card entry
private const val CARD_BORDER_TRACE_OFFSET_MS = 80L // border trace starts after card enters
private const val CARD_BORDER_TRACE_MS = 550        // border trace total duration
private const val LOCKED_DIM_OFFSET_MS = 650L       // after entry → locked cards dim

/**
 * Bottom sheet showing the full 5-tier player progression system.
 *
 * Animation sequence:
 * - t=0   : header typewriter + accent bar grow + divider trace
 * - t=180 : tri-color neon scan line sweeps top→bottom
 * - t=300 : cards drop in one-by-one with spring bounce (130ms stagger)
 * - t=+80 : clockwise border trace per card (PathEffect.dashPathEffect)
 * - t=950 : locked cards dim to LOCKED_ALPHA
 * - t=1400: close button fades in
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelsProgressSheet(
    currentGems: Int,
    onDismiss: () -> Unit,
) {
    val currentLevel = PlayerLevel.fromGems(currentGems)

    // ── Header animations ──────────────────────────────────────────────────────
    val fullTitle = "LEVELS & PROGRESS"
    var displayedTitle by remember { mutableStateOf("") }
    val accentBarHeight = remember { Animatable(0f) }
    val dividerScaleX = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            fullTitle.forEach { char ->
                delay(HEADER_TYPEWRITER_CHAR_MS)
                displayedTitle += char
            }
        }
        launch {
            delay(HEADER_ACCENT_BAR_DELAY_MS)
            accentBarHeight.animateTo(
                targetValue = 24f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        launch {
            delay(HEADER_DIVIDER_DELAY_MS)
            dividerScaleX.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
    }

    // ── Neon scan line ─────────────────────────────────────────────────────────
    val scanProgress = remember { Animatable(0f) }
    var scanDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SCAN_START_DELAY_MS)
        scanProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(SCAN_DURATION_MS, easing = LinearEasing),
        )
        scanDone = true
    }


    HuezooBottomSheet(onDismissRequest = onDismiss) {
        // ── Sticky header — never scrolls away ────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HuezooSpacing.lg)
                .padding(bottom = HuezooSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Accent bar: grows upward from 0 → 24 dp
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(accentBarHeight.value.dp)
                        .background(LocalPlayerAccentColor.current, RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.width(HuezooSpacing.md))
                HuezooTitleLarge(
                    text = displayedTitle,
                    color = HuezooColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(Modifier.height(HuezooSpacing.xs))

            // Tricolor gradient divider — traces from L→R
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .graphicsLayer {
                        scaleX = dividerScaleX.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        alpha = 0.5f
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                HuezooColors.AccentCyan,
                                HuezooColors.AccentMagenta,
                                HuezooColors.AccentYellow,
                            ),
                        ),
                    ),
            )
        }

        // ── Card list + scan overlay + close button ────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HuezooSpacing.lg)
                    .navigationBarsPadding()
                    .padding(top = HuezooSpacing.md)
                    .padding(bottom = HuezooSpacing.xl),
            ) {
                PlayerLevel.entries.forEachIndexed { index, level ->
                    val isCurrentLevel = level == currentLevel
                    val isLocked = level.ordinal > currentLevel.ordinal
                    val enterDelay = CARD_FIRST_DELAY_MS + index * CARD_STAGGER_MS

                    LevelCard(
                        level = level,
                        levelNumber = index + 1,
                        isCurrentLevel = isCurrentLevel,
                        isLocked = isLocked,
                        currentGems = currentGems,
                        enterDelay = enterDelay,
                    )

                    if (index < PlayerLevel.entries.lastIndex) {
                        Spacer(Modifier.height(HuezooSpacing.md))
                    }
                }

                Spacer(Modifier.height(HuezooSpacing.xl))
            }

            // Neon scan line — sweeps once then disappears
            if (!scanDone) {
                ScanLineOverlay(scanProgress = scanProgress.value)
            }
        }
    }
}

/**
 * Tri-color horizontal plasma beam that sweeps top → bottom.
 * A glow halo above the sharp line softens the leading edge.
 */
@Composable
private fun BoxScope.ScanLineOverlay(scanProgress: Float) {
    Canvas(modifier = Modifier.matchParentSize()) {
        val y = size.height * scanProgress
        val beamH = 56.dp.toPx()
        val topY = (y - beamH).coerceAtLeast(0f)

        // Trailing glow gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    HuezooColors.AccentCyan.copy(alpha = 0.07f),
                    HuezooColors.AccentCyan.copy(alpha = 0.14f),
                ),
                startY = topY,
                endY = y,
            ),
            topLeft = Offset(0f, topY),
            size = Size(size.width, y - topY),
        )

        // Sharp tri-color scan line
        drawLine(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to HuezooColors.AccentCyan.copy(alpha = 0f),
                    0.08f to HuezooColors.AccentCyan,
                    0.50f to HuezooColors.AccentMagenta,
                    0.92f to HuezooColors.AccentYellow,
                    1.00f to HuezooColors.AccentYellow.copy(alpha = 0f),
                ),
            ),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

// ── Level card ─────────────────────────────────────────────────────────────────

@Composable
private fun LevelCard(
    level: PlayerLevel,
    levelNumber: Int,
    isCurrentLevel: Boolean,
    isLocked: Boolean,
    currentGems: Int,
    enterDelay: Long,
    modifier: Modifier = Modifier,
) {
    val levelColor = level.levelColor
    val shelfColor = levelColor.copy(alpha = 0.45f)
    val gemRange = gemRangeLabel(level)
    val perks = levelPerks(level)

    // ── Entry animations ───────────────────────────────────────────────────────
    // Values start hidden; spring-bounce into position after `enterDelay`.
    val cardAlpha = remember { Animatable(0f) }
    val cardTranslateYDp = remember { Animatable(64f) }  // dp, converted in graphicsLayer
    val cardScale = remember { Animatable(0.86f) }
    val borderTrace = remember { Animatable(0f) }        // 0=invisible, 1=fully drawn

    LaunchedEffect(Unit) {
        delay(enterDelay)
        launch { cardAlpha.animateTo(1f, tween(280)) }
        launch {
            cardTranslateYDp.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        // Clockwise border trace starts shortly after card begins entering
        launch {
            delay(CARD_BORDER_TRACE_OFFSET_MS)
            borderTrace.animateTo(
                targetValue = 1f,
                animationSpec = tween(CARD_BORDER_TRACE_MS, easing = LinearEasing),
            )
        }
    }

    // Locked cards dim from full-opacity → LOCKED_ALPHA after entry settles
    if (isLocked) {
        LaunchedEffect(Unit) {
            delay(enterDelay + LOCKED_DIM_OFFSET_MS)
            cardAlpha.animateTo(LOCKED_ALPHA, tween(400))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = cardAlpha.value
                scaleX = cardScale.value
                scaleY = cardScale.value
                translationY = cardTranslateYDp.value * density  // dp → px
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shapedShadow(RectangleShape, shelfColor, LevelShelfOffset, LevelShelfOffset)
                .background(HuezooColors.SurfaceL3)
                .drawBehind {
                    val borderStroke = if (isCurrentLevel) 3.dp.toPx() else 2.dp.toPx()
                    val borderAlpha = if (isCurrentLevel) 0.85f else 0.50f
                    val accentH = if (isCurrentLevel) 4.dp.toPx() else 3.dp.toPx()

                    // ── Clockwise border trace via PathEffect.dashPathEffect ──────
                    // Perimeter = 2*(W+H). Phase animates from perimeter→0:
                    //  phase=P → nothing drawn; phase=0 → full rect drawn.
                    val perimeter = 2f * (size.width + size.height)
                    val phase = perimeter * (1f - borderTrace.value)
                    drawRect(
                        color = levelColor.copy(alpha = borderAlpha),
                        style = Stroke(
                            width = borderStroke,
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(perimeter, perimeter),
                                phase = phase,
                            ),
                        ),
                    )

                    // ── Kinetic top accent strip — sweeps L→R in sync with border ──
                    drawRect(
                        color = levelColor.copy(alpha = if (isCurrentLevel) 0.55f else 0.35f),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width * borderTrace.value, accentH),
                    )
                }
                .padding(HuezooSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            // ── Card header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    HuezooLabelSmall(
                        text = "LEVEL ${levelNumber.toString().padStart(2, '0')}",
                        color = levelColor,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    HuezooHeadlineMedium(
                        text = level.displayName,
                        color = HuezooColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Box(
                    modifier = Modifier
                        .background(levelColor.copy(alpha = 0.18f))
                        .padding(horizontal = HuezooSpacing.sm, vertical = HuezooSpacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    HuezooLabelSmall(
                        text = if (isCurrentLevel) "CURRENT" else if (isLocked) "LOCKED" else "CLEARED",
                        color = levelColor,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // ── Gem threshold row ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HuezooColors.SurfaceL0)
                    .padding(horizontal = HuezooSpacing.sm, vertical = HuezooSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HuezooLabelSmall(
                    text = "THRESHOLD",
                    color = HuezooColors.TextDisabled,
                    fontWeight = FontWeight.SemiBold,
                )
                HuezooTitleSmall(
                    text = gemRange,
                    color = levelColor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            // ── What Changes section ───────────────────────────────────────────
            HuezooLabelSmall(
                text = "WHAT CHANGES",
                color = HuezooColors.TextDisabled,
                fontWeight = FontWeight.SemiBold,
            )

            perks.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HuezooColors.SurfaceL0)
                        .padding(horizontal = HuezooSpacing.sm, vertical = HuezooSpacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HuezooBodyMedium(
                        text = label,
                        color = HuezooColors.TextSecondary,
                    )
                    HuezooBodyMedium(
                        text = value,
                        color = levelColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ── Progress bar (current level only) ─────────────────────────────
            if (isCurrentLevel) {
                Spacer(Modifier.height(HuezooSpacing.xs))
                LevelProgressInCard(
                    level = level,
                    currentGems = currentGems,
                    levelColor = levelColor,
                    animationDelay = enterDelay + 350L,
                )
            }
        }
    }
}

@Composable
private fun LevelProgressInCard(
    level: PlayerLevel,
    currentGems: Int,
    levelColor: Color,
    animationDelay: Long,
    modifier: Modifier = Modifier,
) {
    val nextLevel = PlayerLevel.entries.getOrNull(level.ordinal + 1)
    val targetFraction = if (nextLevel != null) {
        val range = (nextLevel.minGems - level.minGems).toFloat()
        val progress = (currentGems - level.minGems).toFloat()
        (progress / range).coerceIn(0f, 1f)
    } else {
        1f
    }

    // Charge-up animation: 0 → actual fraction after card has entered
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(animationDelay)
        animatedProgress.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(900, easing = EaseOutCubic),
        )
    }

    val trackShape = RoundedCornerShape(4.dp)
    val clampedProgress = animatedProgress.value.coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Progress bar: taller (8 dp) with horizontal glow gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(HuezooColors.SurfaceL0, trackShape)
                .clip(trackShape),
        ) {
            // Glow halo — faint → bright toward fill edge
            if (clampedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clampedProgress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    levelColor.copy(alpha = 0.15f),
                                    levelColor.copy(alpha = 0.50f),
                                ),
                            ),
                        ),
                )
            }
            // Solid fill bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .fillMaxHeight(0.55f)
                    .align(Alignment.CenterStart)
                    .background(levelColor, trackShape),
            )
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HuezooLabelSmall(
                text = "$currentGems GEMS",
                color = levelColor,
            )
            if (nextLevel != null) {
                val gemsToNext = nextLevel.minGems - currentGems
                HuezooLabelSmall(
                    text = "$gemsToNext to ${nextLevel.displayName}",
                    color = HuezooColors.TextDisabled,
                )
            } else {
                HuezooLabelSmall(
                    text = "MAX LEVEL",
                    color = levelColor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

// ── Data helpers ───────────────────────────────────────────────────────────────

private fun gemRangeLabel(level: PlayerLevel): String {
    val next = PlayerLevel.entries.getOrNull(level.ordinal + 1)
    return if (next != null) {
        "${formatGems(level.minGems)} – ${formatGems(next.minGems - 1)} GEMS"
    } else {
        "${formatGems(level.minGems)}+ GEMS"
    }
}

private fun formatGems(n: Int): String = when {
    n >= 1_000 -> "${n / 1_000},${(n % 1_000).toString().padStart(3, '0')}"
    else -> "$n"
}

/**
 * Placeholder "what changes" per level — values TBD once monetization is finalized.
 * See GAME_DESIGN.md Open Design Questions.
 */
private fun levelPerks(level: PlayerLevel): List<Pair<String, String>> = when (level) {
    PlayerLevel.Rookie -> listOf(
        "Difficulty" to "Standard",
        "Rewards" to "1.0× GEMS",
        "Badge" to "Default",
    )
    PlayerLevel.Trained -> listOf(
        "Difficulty" to "Wider gamut",
        "Rewards" to "1.0× GEMS",
        "Badge" to "Glow frame",
    )
    PlayerLevel.Sharp -> listOf(
        "Difficulty" to "Subtle shifts",
        "Rewards" to "1.2× GEMS",
        "Badge" to "Neon aura",
    )
    PlayerLevel.Elite -> listOf(
        "Difficulty" to "Near-invisible",
        "Rewards" to "1.5× GEMS",
        "Badge" to "Elite insignia",
    )
    PlayerLevel.Master -> listOf(
        "Difficulty" to "Chromatic limits",
        "Rewards" to "2.0× GEMS",
        "Badge" to "Master crown",
    )
}

// ── Previews ───────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun LevelsProgressSheetRookiePreview() {
    HuezooPreviewTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HuezooColors.SurfaceL2)
                .padding(HuezooSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
        ) {
            PlayerLevel.entries.forEachIndexed { index, level ->
                LevelCard(
                    level = level,
                    levelNumber = index + 1,
                    isCurrentLevel = level == PlayerLevel.Rookie,
                    isLocked = level.ordinal > PlayerLevel.Rookie.ordinal,
                    currentGems = 87,
                    enterDelay = 0L,
                )
            }
        }
    }
}

@PreviewComponent
@Composable
private fun LevelsProgressSheetSharpPreview() {
    HuezooPreviewTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HuezooColors.SurfaceL2)
                .padding(HuezooSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
        ) {
            PlayerLevel.entries.forEachIndexed { index, level ->
                LevelCard(
                    level = level,
                    levelNumber = index + 1,
                    isCurrentLevel = level == PlayerLevel.Sharp,
                    isLocked = level.ordinal > PlayerLevel.Sharp.ordinal,
                    currentGems = 2100,
                    enterDelay = 0L,
                )
            }
        }
    }
}
