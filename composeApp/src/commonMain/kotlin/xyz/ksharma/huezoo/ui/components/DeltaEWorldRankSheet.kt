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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.model.PERCEPTION_TIERS
import xyz.ksharma.huezoo.ui.model.PerceptionTier
import xyz.ksharma.huezoo.ui.model.estimatedPerceptionTier
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Animation timing ───────────────────────────────────────────────────────────
private const val HEADER_CHAR_MS = 35L
private const val HEADER_BAR_DELAY_MS = 50L
private const val HEADER_DIVIDER_DELAY_MS = 120L
private const val SCAN_START_MS = 180L
private const val SCAN_DUR_MS = 450
private const val HERO_ENTER_MS = 350L
private const val ARC_START_MS = 540L
private const val ARC_DUR_MS = 1300
private const val TIER_FIRST_MS = 720L
private const val TIER_STAGGER_MS = 110L
private const val TIER_BORDER_OFFSET_MS = 80L
private const val TIER_BORDER_DUR_MS = 550

// ── Helpers ────────────────────────────────────────────────────────────────────

/** Percentage of the world the player out-ranks for a given tier.
 *  e.g. "TOP 5%" → beats 95% of all players. */
private fun beatPct(tier: PerceptionTier): Int {
    val x = tier.rankLabel.removePrefix("TOP ").removeSuffix("%").trim().toIntOrNull() ?: 50
    return 100 - x
}

private fun Float.fmtDE(): String {
    val i = toInt()
    val d = ((this - i) * 10).toInt()
    return "$i.$d"
}

// ── Public sheet ───────────────────────────────────────────────────────────────

/**
 * Bottom sheet shown when the player taps the ΔE card on the Result screen.
 *
 * Animation sequence:
 * - t=0   : header typewriter + accent-bar grow + divider trace L→R
 * - t=180 : tier-colored scan line sweeps top → bottom once
 * - t=350 : hero rank badge fades + spring-scales in
 * - t=540 : semicircle arc gauge fills (1.3 s EaseOutCubic)
 *           "YOU BEAT X%" counter ticks up in sync
 * - t=720 : tier rows spring-drop in, 110 ms stagger
 *           player's row gets clockwise border-trace highlight
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeltaEWorldRankSheet(
    deltaE: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tier = remember(deltaE) { estimatedPerceptionTier(deltaE) }
    val beat = remember(tier) { beatPct(tier) }

    // ── Header typewriter ──────────────────────────────────────────────────────
    val fullTitle = "ΔE WORLD RANK"
    var displayedTitle by remember { mutableStateOf("") }
    val accentBarH = remember { Animatable(0f) }
    val dividerScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            fullTitle.forEach { c ->
                delay(HEADER_CHAR_MS)
                displayedTitle += c
            }
        }
        launch {
            delay(HEADER_BAR_DELAY_MS)
            accentBarH.animateTo(
                targetValue = 24f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        launch {
            delay(HEADER_DIVIDER_DELAY_MS)
            dividerScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
    }

    // ── Scan line ──────────────────────────────────────────────────────────────
    val scanProgress = remember { Animatable(0f) }
    var scanDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SCAN_START_MS)
        scanProgress.animateTo(1f, tween(SCAN_DUR_MS, easing = LinearEasing))
        scanDone = true
    }

    // ── Hero entrance ──────────────────────────────────────────────────────────
    val heroAlpha = remember { Animatable(0f) }
    val heroScale = remember { Animatable(0.82f) }
    LaunchedEffect(Unit) {
        delay(HERO_ENTER_MS)
        launch { heroAlpha.animateTo(1f, tween(380)) }
        launch {
            heroScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 200f),
            )
        }
    }

    // ── Arc gauge fill ─────────────────────────────────────────────────────────
    val arcFraction = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(ARC_START_MS)
        arcFraction.animateTo(beat / 100f, tween(ARC_DUR_MS, easing = EaseOutCubic))
    }

    HuezooBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        // ── Sticky header ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HuezooSpacing.lg)
                .padding(bottom = HuezooSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(accentBarH.value.dp)
                        .background(tier.color, RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.width(HuezooSpacing.md))
                HuezooTitleLarge(
                    text = displayedTitle,
                    color = HuezooColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.height(HuezooSpacing.xs))
            // Tri-color divider traces left → right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .graphicsLayer {
                        scaleX = dividerScale.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        alpha = 0.55f
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                tier.color,
                                HuezooColors.AccentMagenta,
                                HuezooColors.AccentYellow,
                            ),
                        ),
                    ),
            )
        }

        // ── Scrollable body ────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HuezooSpacing.lg)
                    .navigationBarsPadding()
                    .padding(top = HuezooSpacing.sm)
                    .padding(bottom = HuezooSpacing.xl),
            ) {
                // ── Arc gauge hero ─────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = heroAlpha.value
                            scaleX = heroScale.value
                            scaleY = heroScale.value
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    PercentileArcGauge(
                        arcFraction = arcFraction.value,
                        tierColor = tier.color,
                        rankLabel = tier.rankLabel,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(HuezooSpacing.sm))

                // ── Session ΔE readout ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HuezooColors.SurfaceL3, RoundedCornerShape(HuezooSize.CornerCard))
                        .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HuezooLabelSmall(
                        text = "YOUR SESSION ΔE",
                        color = HuezooColors.TextDisabled,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    HuezooTitleLarge(
                        text = "ΔE ${deltaE.fmtDE()}",
                        color = tier.color,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Spacer(Modifier.height(HuezooSpacing.md))

                // ── Tier ladder ────────────────────────────────────────────────
                HuezooLabelSmall(
                    text = "ALL PERCEPTION TIERS",
                    color = HuezooColors.TextDisabled,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(Modifier.height(HuezooSpacing.sm))

                PERCEPTION_TIERS.forEachIndexed { i, rankTier ->
                    RankTierRow(
                        tier = rankTier,
                        isCurrentTier = rankTier == tier,
                        enterDelay = TIER_FIRST_MS + i * TIER_STAGGER_MS,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (i < PERCEPTION_TIERS.lastIndex) {
                        Spacer(Modifier.height(HuezooSpacing.xs))
                    }
                }

                Spacer(Modifier.height(HuezooSpacing.md))

                HuezooLabelSmall(
                    text = "Measured using CIEDE2000 — the international standard for human color difference perception.",
                    color = HuezooColors.TextDisabled,
                    maxLines = Int.MAX_VALUE,
                )
            }

            // Scan line — renders once, then disappears
            if (!scanDone) {
                WorldRankScanLine(
                    scanProgress = scanProgress.value,
                    tierColor = tier.color,
                )
            }
        }
    }
}

// ── Percentile arc gauge ──────────────────────────────────────────────────────

/**
 * Animated semicircle gauge. Arc fills from left → right as [arcFraction] grows 0→beatPct/100.
 * Center text counts up in sync: "YOU BEAT X%  ·  RANK LABEL".
 */
@Composable
private fun PercentileArcGauge(
    arcFraction: Float,
    tierColor: Color,
    rankLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(210.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            // Arc center placed at bottom edge so only upper half (semicircle) is visible
            val cy = size.height
            val radius = minOf(cx, cy) * 0.88f
            val strokeW = 14.dp.toPx()

            // ── Background track ──────────────────────────────────────────────
            drawArc(
                color = HuezooColors.SurfaceL3,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(strokeW, cap = StrokeCap.Round),
            )

            val sweep = 180f * arcFraction

            if (sweep > 0.5f) {
                // ── Glow halo ─────────────────────────────────────────────────
                drawArc(
                    color = tierColor.copy(alpha = 0.18f),
                    startAngle = 180f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(strokeW * 2.6f, cap = StrokeCap.Round),
                )
                // ── Solid arc ─────────────────────────────────────────────────
                drawArc(
                    color = tierColor,
                    startAngle = 180f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(strokeW, cap = StrokeCap.Round),
                )
                // ── Endpoint glow dot ─────────────────────────────────────────
                val endRad = (180.0 + sweep) * (PI / 180.0)
                val dotX = cx + radius * cos(endRad).toFloat()
                val dotY = cy + radius * sin(endRad).toFloat()
                drawCircle(
                    color = tierColor.copy(alpha = 0.40f),
                    radius = strokeW * 1.7f,
                    center = Offset(dotX, dotY),
                )
                drawCircle(
                    color = Color.White,
                    radius = strokeW * 0.48f,
                    center = Offset(dotX, dotY),
                )
                drawCircle(
                    color = tierColor,
                    radius = strokeW * 0.32f,
                    center = Offset(dotX, dotY),
                )
            }
        }

        // Center text counts up in sync with arc fill
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = HuezooSpacing.xl),
        ) {
            HuezooLabelSmall(
                text = "YOU BEAT",
                color = HuezooColors.TextDisabled,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            androidx.compose.material3.Text(
                text = "${(arcFraction * 100).toInt()}%",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    lineHeight = 72.sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = tierColor,
                textAlign = TextAlign.Center,
            )
            HuezooLabelSmall(
                text = "OF THE WORLD  ·  $rankLabel",
                color = tierColor.copy(alpha = 0.72f),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Animated tier row ─────────────────────────────────────────────────────────

/**
 * Each row spring-drops in from [enterDelay].
 * The player's current tier row gets a clockwise border-trace highlight.
 */
@Composable
private fun RankTierRow(
    tier: PerceptionTier,
    isCurrentTier: Boolean,
    enterDelay: Long,
    modifier: Modifier = Modifier,
) {
    val rowAlpha = remember { Animatable(0f) }
    val rowTransY = remember { Animatable(28f) }   // dp
    val borderTrace = remember { Animatable(0f) }  // 0=invisible → 1=fully traced

    LaunchedEffect(Unit) {
        delay(enterDelay)
        launch { rowAlpha.animateTo(1f, tween(260)) }
        launch {
            rowTransY.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        if (isCurrentTier) {
            launch {
                delay(TIER_BORDER_OFFSET_MS)
                borderTrace.animateTo(1f, tween(TIER_BORDER_DUR_MS, easing = LinearEasing))
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = rowAlpha.value
            translationY = rowTransY.value * density
        },
    ) {
        if (isCurrentTier) {
            // Highlighted card with border-trace animation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HuezooColors.SurfaceL3, RoundedCornerShape(HuezooSize.CornerCard))
                    .drawBehind {
                        val cornerPx = HuezooSize.CornerCard.toPx()
                        val perimeter = 2f * (size.width + size.height)
                        val phase = perimeter * (1f - borderTrace.value)
                        // Clockwise border trace
                        drawRoundRect(
                            color = tier.color.copy(alpha = 0.82f),
                            cornerRadius = CornerRadius(cornerPx),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(perimeter, perimeter),
                                    phase = phase,
                                ),
                            ),
                        )
                        // Kinetic top-accent strip sweeps L→R in sync with border
                        drawRoundRect(
                            color = tier.color.copy(alpha = 0.42f),
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width * borderTrace.value, 4.dp.toPx()),
                            cornerRadius = CornerRadius(cornerPx),
                        )
                    }
                    .padding(HuezooSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tier.color),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    HuezooLabelSmall(
                        text = tier.rankLabel,
                        color = tier.color,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    HuezooLabelSmall(
                        text = tier.description,
                        color = HuezooColors.TextSecondary,
                        maxLines = Int.MAX_VALUE,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    HuezooLabelSmall(
                        text = tier.deltaERange,
                        color = tier.color.copy(alpha = 0.82f),
                        fontWeight = FontWeight.ExtraBold,
                    )
                    HuezooLabelSmall(
                        text = "◀ YOU",
                        color = tier.color,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        } else {
            // Dimmed non-highlighted row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HuezooSpacing.sm, vertical = HuezooSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tier.color.copy(alpha = 0.42f)),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    HuezooLabelSmall(
                        text = tier.rankLabel,
                        color = tier.color.copy(alpha = 0.60f),
                        fontWeight = FontWeight.ExtraBold,
                    )
                    HuezooLabelSmall(
                        text = tier.description,
                        color = HuezooColors.TextDisabled,
                        maxLines = Int.MAX_VALUE,
                    )
                }
                HuezooLabelSmall(
                    text = tier.deltaERange,
                    color = HuezooColors.TextDisabled,
                )
            }
        }
    }
}

// ── Scan line ─────────────────────────────────────────────────────────────────

/** Tri-color plasma beam that sweeps top → bottom once and disappears. */
@Composable
private fun BoxScope.WorldRankScanLine(scanProgress: Float, tierColor: Color) {
    Canvas(modifier = Modifier.matchParentSize()) {
        val y = size.height * scanProgress
        val beamH = 56.dp.toPx()
        val topY = (y - beamH).coerceAtLeast(0f)

        // Trailing glow gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    tierColor.copy(alpha = 0.07f),
                    tierColor.copy(alpha = 0.16f),
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
                    0.00f to tierColor.copy(alpha = 0f),
                    0.08f to tierColor,
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

