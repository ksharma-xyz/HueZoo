package xyz.ksharma.huezoo.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.ksharma.huezoo.ui.components.HuezooBodyMedium
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooDisplayMedium
import xyz.ksharma.huezoo.ui.components.HuezooHeadlineSmall
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTitleLarge
import xyz.ksharma.huezoo.ui.components.HuezooTitleSmall
import xyz.ksharma.huezoo.ui.home.state.DailyCardData
import xyz.ksharma.huezoo.ui.home.state.ThresholdCardData
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.ParallelogramBack
import xyz.ksharma.huezoo.ui.theme.rimLight
import xyz.ksharma.huezoo.ui.theme.shapedShadow
import xyz.ksharma.huezoo.ui.theme.shimmerCelebration
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val ShelfColor = HuezooColors.SurfaceL4
private val ShelfOffset = 4.dp
private val IconBoxSize = 80.dp
private val CyanAccentBarWidth = 6.dp

// ── Stats section ─────────────────────────────────────────────────────────────

/**
 * Stats section layout (mobile-first):
 *
 * CURRENT INVENTORY  ← label above, outside the box
 * ┌─────────────────────────────┐
 * │▌ 1,250  GEMS                │  ← level-color left accent bar + neo-brutal shadow
 * └─────────────────────────────┘
 * ┌─────────────────────────────┐
 * │ STREAK                      │
 * │ 0 DAYS                      │
 * └─────────────────────────────┘
 */
@Composable
internal fun StatsSection(
    totalGems: Int,
    streak: Int,
    onGemsClick: () -> Unit,
    isStreakCelebrating: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
    ) {
        HuezooLabelSmall(
            text = "CURRENT INVENTORY",
            color = HuezooColors.TextDisabled,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .shapedShadow(RectangleShape, ShelfColor, ShelfOffset, ShelfOffset),
        ) {
            Box(
                modifier = Modifier
                    .width(CyanAccentBarWidth)
                    .fillMaxHeight()
                    .background(LocalPlayerAccentColor.current),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(HuezooColors.SurfaceL2)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onGemsClick,
                    )
                    .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.sm + 4.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                GemSpillIllustration(modifier = Modifier.matchParentSize())
                Row {
                    HuezooDisplayMedium(
                        text = formatGems(totalGems),
                        color = HuezooColors.TextPrimary,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(HuezooSpacing.xs))
                    HuezooLabelSmall(
                        text = "GEMS",
                        color = LocalPlayerAccentColor.current,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }

        StatBox(
            label = "STREAK",
            value = "$streak DAYS",
            hint = "Daily challenge counts toward streak",
            accentColor = HuezooColors.AccentMagenta,
            celebrate = isStreakCelebrating,
            modifier = Modifier.fillMaxWidth().padding(top = HuezooSpacing.md),
        )
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    accentColor: Color,
    celebrate: Boolean = false,
    hint: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shapedShadow(RectangleShape, ShelfColor, ShelfOffset, ShelfOffset)
            .background(HuezooColors.SurfaceL0)
            .shimmerCelebration(active = celebrate, glowColor = accentColor)
            .drawBehind {
                drawRect(
                    color = accentColor.copy(alpha = 0.55f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 2.dp.toPx()),
                )
            }
            .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.md),
    ) {
        Column {
            HuezooLabelSmall(
                text = label,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(2.dp))
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (
                        scaleIn(initialScale = 1.45f, animationSpec = tween(280)) +
                            fadeIn(tween(220))
                        )
                        .togetherWith(
                            scaleOut(targetScale = 0.65f, animationSpec = tween(180)) +
                                fadeOut(tween(180)),
                        )
                },
                label = "statBoxValue",
            ) { animatedValue ->
                HuezooHeadlineSmall(
                    text = animatedValue,
                    color = HuezooColors.TextPrimary,
                )
            }
            if (hint != null) {
                Spacer(Modifier.height(3.dp))
                HuezooLabelSmall(
                    text = hint,
                    color = Color.White.copy(alpha = 0.38f),
                )
            }
        }
    }
}

// ── Level progress bar ────────────────────────────────────────────────────────

@Composable
internal fun LevelProgressBar(
    fraction: Float,
    currentLevel: PlayerLevel,
    totalGems: Int,
    accentColor: Color,
    onClick: () -> Unit,
    alreadyPlayed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val nextLevel = PlayerLevel.entries.getOrNull(currentLevel.ordinal + 1)
    val rightLabel = nextLevel?.let { "${formatGems(totalGems)} / ${formatGems(it.minGems)}" }

    // Cold-open charge-up: starts at 0 and fills to actual fraction on first open only.
    val animatedFraction = remember {
        Animatable(if (alreadyPlayed) fraction else 0f)
    }
    LaunchedEffect(Unit) {
        if (!alreadyPlayed) {
            delay(INITIAL_DELAY_MS + 400L)
            animatedFraction.animateTo(fraction, tween(700, easing = EaseOutCubic))
        }
    }
    // Keep in sync when gems change mid-session (e.g. back from a game)
    LaunchedEffect(fraction) {
        if (alreadyPlayed) animatedFraction.snapTo(fraction)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = HuezooSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(HuezooColors.SurfaceL4),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction.value.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(accentColor),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HuezooLabelSmall(text = currentLevel.displayName, color = accentColor)
            if (rightLabel != null) {
                HuezooLabelSmall(text = rightLabel, color = HuezooColors.TextDisabled)
            }
        }
    }
}

// ── Threshold hero card ───────────────────────────────────────────────────────

/**
 * Full-width hero card. The CARD ITSELF is NOT tappable — only the
 * "ENTER SIMULATION" button navigates to the game.
 */
@OptIn(ExperimentalTime::class)
@Composable
internal fun ThresholdHeroCard(
    data: ThresholdCardData,
    isPaid: Boolean,
    onEnterGame: () -> Unit,
    onWatchAd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val triesText = when {
        isPaid -> "∞  UNLIMITED"
        data.isBlocked -> "OUT OF TRIES"
        else -> "${data.attemptsRemaining} / ${data.maxAttempts} TRIES REMAINING"
    }
    val countdown = data.nextResetAt?.let { countdownUntil(it, prefix = "Resets in ") }
    val enabled = !data.isBlocked

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shapedShadow(RectangleShape, ShelfColor, ShelfOffset, ShelfOffset)
            .background(HuezooColors.SurfaceL2)
            .rimLight(cornerRadius = 0.dp),
    ) {
        ThresholdScannerIllustration(
            enabled = enabled,
            modifier = Modifier.matchParentSize(),
        )

        Column(modifier = Modifier.fillMaxWidth().padding(HuezooSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val accentForDot = LocalPlayerAccentColor.current
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(
                        color = if (enabled) {
                            accentForDot.copy(alpha = pulseAlpha)
                        } else {
                            HuezooColors.TextDisabled
                        },
                        radius = size.minDimension / 2f,
                    )
                }
                Spacer(Modifier.width(HuezooSpacing.sm))
                HuezooLabelSmall(
                    text = if (enabled) "ACTIVE MISSION" else "MISSION LOCKED",
                    color = if (enabled) LocalPlayerAccentColor.current else HuezooColors.TextDisabled,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(Modifier.height(HuezooSpacing.md))
            HuezooTitleLarge(
                text = "THE\nTHRESHOLD",
                color = if (enabled) HuezooColors.TextPrimary else HuezooColors.TextSecondary,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(HuezooSpacing.sm))
            HuezooBodyMedium(
                text = "Analyze chromatic anomalies.\nDetect the odd color — precision test for the elite observer.",
                color = HuezooColors.TextSecondary,
                maxLines = 3,
            )
            Spacer(Modifier.height(HuezooSpacing.lg))
            HuezooLabelSmall(
                text = countdown ?: triesText,
                color = if (enabled) HuezooColors.GameThreshold else HuezooColors.TextDisabled,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )
            data.personalBestDeltaE?.let { de ->
                Spacer(Modifier.height(2.dp))
                HuezooLabelSmall(
                    text = "BEST: ΔE ${((de * 10).toInt() / 10.0)}",
                    color = HuezooColors.TextDisabled,
                )
            }
            Spacer(Modifier.height(HuezooSpacing.md))
            if (enabled) {
                HuezooButton(
                    text = "ENTER SIMULATION",
                    onClick = onEnterGame,
                    variant = HuezooButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                HuezooLabelSmall(
                    text = "NO TRIES LEFT",
                    color = HuezooColors.AccentMagenta,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(HuezooSpacing.sm))
                HuezooButton(
                    text = "WATCH AD — EARN +1 TRY",
                    onClick = onWatchAd,
                    variant = HuezooButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ── Compact cards ─────────────────────────────────────────────────────────────

/**
 * Daily Challenge compact card — full width, stacked below Threshold.
 * Clickable when daily is not yet completed today.
 * When available, shows an animated neon cat and streak context in the subtitle.
 */
@OptIn(ExperimentalTime::class)
@Composable
internal fun DailyCompactCard(
    data: DailyCardData,
    challengeName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    streak: Int = 0,
) {
    val countdown = data.nextPuzzleAt?.let { countdownUntil(it, prefix = "Next in ") }
    val subtitleText = when {
        data.isCompletedToday -> countdown ?: "Completed today"
        streak > 0 -> "$streak-day streak — play to extend it!"
        else -> "Plays count toward your streak"
    }
    CompactCard(
        label = "DAILY CHALLENGE",
        title = challengeName,
        subtitle = subtitleText,
        accentColor = HuezooColors.AccentMagenta,
        enabled = !data.isCompletedToday,
        onClick = onClick,
        iconDraw = if (data.isCompletedToday) {
            { color -> drawMedalStar(color) }
        } else {
            null
        },
        iconContent = if (!data.isCompletedToday) {
            { AnimatedDailyIcon(HuezooColors.AccentMagenta) }
        } else {
            null
        },
        modifier = modifier,
    )
}

/** Global Leaderboard compact card — shows player's estimated rank tier. */
@Composable
internal fun LeaderboardCompactCard(
    personalBestDeltaE: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rankLabel = personalBestDeltaE?.let { estimatedRankLabel(it) } ?: "N/A"
    val rankDescription = personalBestDeltaE?.let { estimatedRankDescription(it) } ?: "Play Threshold to rank"
    CompactCard(
        label = "GLOBAL LEADERBOARD",
        title = rankLabel,
        subtitle = rankDescription,
        accentColor = HuezooColors.AccentYellow,
        enabled = true,
        onClick = onClick,
        iconDraw = { color -> drawLeaderboardBars(color) },
        modifier = modifier,
    )
}

/**
 * Compact card:
 * - Left: fixed-size icon box with neon top-left rim + icon drawn via Canvas
 * - Right: label / title / subtitle
 * - Neo-brutalist top-border accent + shelf shadow
 *
 * Pass [iconContent] to replace the static Canvas draw with a live Composable (e.g. animation).
 */
@Composable
private fun CompactCard(
    label: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconDraw: (DrawScope.(Color) -> Unit)? = null,
    iconContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .shapedShadow(RectangleShape, ShelfColor, ShelfOffset, ShelfOffset)
            .background(HuezooColors.SurfaceL2)
            .drawBehind {
                drawRect(
                    color = accentColor.copy(alpha = 0.55f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 2.dp.toPx()),
                )
            }
            .padding(HuezooSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(IconBoxSize)
                .background(HuezooColors.SurfaceL0)
                .drawBehind {
                    val stroke = 1.5.dp.toPx()
                    drawLine(
                        accentColor.copy(alpha = 0.5f),
                        Offset(0f, stroke / 2f),
                        Offset(size.width, stroke / 2f),
                        stroke,
                    )
                    drawLine(
                        accentColor.copy(alpha = 0.5f),
                        Offset(stroke / 2f, 0f),
                        Offset(stroke / 2f, size.height),
                        stroke,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (iconContent != null) {
                iconContent()
            } else if (iconDraw != null) {
                Canvas(modifier = Modifier.size(36.dp)) {
                    iconDraw(accentColor)
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            HuezooLabelSmall(
                text = label,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            HuezooTitleSmall(
                text = title,
                color = HuezooColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            HuezooLabelSmall(
                text = subtitle,
                color = HuezooColors.TextDisabled,
            )
        }
    }
}

// ── Icon canvas drawers ───────────────────────────────────────────────────────

private fun DrawScope.drawMedalStar(color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outer = size.minDimension * 0.48f
    val inner = outer * 0.40f
    val path = Path()
    for (i in 0..9) {
        val angle = (i * 36.0 - 90.0) * PI / 180.0
        val r = if (i % 2 == 0) outer else inner
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}

private fun DrawScope.drawLeaderboardBars(color: Color) {
    val barW = size.width * 0.24f
    val gap = size.width * 0.08f
    val totalW = barW * 3 + gap * 2
    val startX = (size.width - totalW) / 2f
    listOf(0.45f, 0.70f, 1.0f).forEachIndexed { i, h ->
        val barH = size.height * h
        drawRect(
            color = color,
            topLeft = Offset(startX + i * (barW + gap), size.height - barH),
            size = Size(barW, barH),
        )
    }
}

// ── Player ΔE card ────────────────────────────────────────────────────────────

/**
 * Compact ΔE personal-best card.
 *
 * Layout: left = label + subtitle | right = big ΔE value (or "N/A")
 * Visual: right-side only accent border (3 dp vertical bar).
 */
@Composable
internal fun PlayerDeltaECard(
    bestDeltaE: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalPlayerAccentColor.current
    val cardShape = RoundedCornerShape(HuezooSize.CornerCard)
    val hasPlayed = bestDeltaE != null

    Row(
        modifier = modifier
            .background(HuezooColors.SurfaceL2, cardShape)
            .rimLight(cornerRadius = HuezooSize.CornerCard)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .drawBehind {
                val barWidth = 3.dp.toPx()
                val cornerPx = HuezooSize.CornerCard.toPx()
                drawLine(
                    color = accent,
                    start = Offset(size.width - barWidth / 2f, cornerPx),
                    end = Offset(size.width - barWidth / 2f, size.height - cornerPx),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round,
                )
            }
            .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            HuezooLabelSmall(
                text = "PERSONAL BEST ΔE",
                color = accent,
                fontWeight = FontWeight.ExtraBold,
            )
            HuezooLabelSmall(
                text = if (hasPlayed) "Lower is sharper color vision" else "Play Threshold to set a score",
                color = HuezooColors.TextDisabled,
            )
        }

        androidx.compose.material3.Text(
            text = if (hasPlayed) bestDeltaE.fmtHome() else "N/A",
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
            color = if (hasPlayed) accent else HuezooColors.TextDisabled,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = HuezooSpacing.md),
        )
    }
}

// ── ΔE info card ──────────────────────────────────────────────────────────────

@Composable
internal fun DeltaEInfoCard(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "chevron",
    )

    val cardShape = RoundedCornerShape(HuezooSize.CornerCard)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HuezooColors.SurfaceL2, cardShape)
            .border(1.5.dp, LocalPlayerAccentColor.current.copy(alpha = 0.28f), cardShape)
            .clip(cardShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded },
                )
                .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.sm + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val deltaEAccent = LocalPlayerAccentColor.current
            HuezooLabelSmall(
                text = "WHAT IS ΔE?",
                color = deltaEAccent,
                fontWeight = FontWeight.ExtraBold,
            )
            Canvas(
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
            ) {
                val sw = 2.5.dp.toPx()
                val w = size.width
                val h = size.height
                drawLine(
                    deltaEAccent.copy(alpha = 0.7f),
                    Offset(w * 0.15f, h * 0.38f),
                    Offset(w * 0.50f, h * 0.65f),
                    sw,
                    StrokeCap.Round,
                )
                drawLine(
                    deltaEAccent.copy(alpha = 0.7f),
                    Offset(w * 0.50f, h * 0.65f),
                    Offset(w * 0.85f, h * 0.38f),
                    sw,
                    StrokeCap.Round,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(400)),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = HuezooSpacing.md,
                    end = HuezooSpacing.md,
                    bottom = HuezooSpacing.md,
                ),
            ) {
                HuezooBodyMedium(
                    text = "ΔE (Delta-E) measures how different two colors look to the human eye. " +
                        "Unlike raw RGB math, ΔE is perceptual — it accounts for how our eyes " +
                        "actually see color.\n\n" +
                        "ΔE 10: red vs blue — instantly obvious\n" +
                        "ΔE 3:  clearly different if side by side\n" +
                        "ΔE 1:  just noticeable to a trained eye\n" +
                        "ΔE 0.5: near the limit of human vision\n\n" +
                        "Huezoo uses CIEDE2000 — the international color science standard used by " +
                        "paint manufacturers, display calibrators, and medical imaging. " +
                        "Your score here reflects real perceptual precision.",
                    color = HuezooColors.TextSecondary,
                )
                Spacer(Modifier.height(HuezooSpacing.md))
                HuezooBodyMedium(
                    text = "Reference: ISO 11664-6 / CIE 142-2001 (CIEDE2000).",
                    color = HuezooColors.TextDisabled,
                )
                Spacer(Modifier.height(HuezooSpacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.xs),
                ) {
                    val tierAccent = LocalPlayerAccentColor.current
                    listOf("≥5 EASY", "2–5 MED", "1–2 HARD", "<1 ELITE").forEach { tier ->
                        HuezooLabelSmall(
                            text = tier,
                            color = tierAccent.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

// ── Upgrade CTA ──────────────────────────────────────────────────────────────

/**
 * Full-width parallelogram "GET FULL ACCESS" button.
 * Placed below ThresholdHeroCard when the user is blocked (free tier, no tries left).
 */
@Composable
internal fun UpgradeCta(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = if (isPressed) {
            tween(80)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "upgradeCtaPress",
    )

    val shelfHeight = 5.dp
    val shelfPx = with(LocalDensity.current) { shelfHeight.toPx() }

    Box(modifier = modifier.fillMaxWidth().padding(bottom = shelfHeight)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shelfHeight)
                .clip(ParallelogramBack)
                .background(HuezooColors.ShelfPrice),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .graphicsLayer { translationY = pressProgress * shelfPx }
                .clip(ParallelogramBack)
                .background(HuezooColors.PriceGreen)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            HuezooLabelSmall(
                text = "GET FULL ACCESS",
                color = HuezooColors.SurfaceL0,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

internal fun formatGems(gems: Int): String = when {
    gems >= 1_000 -> "${gems / 1_000},${(gems % 1_000).toString().padStart(3, '0')}"
    else -> "$gems"
}

private fun Float.fmtHome(): String {
    val i = toInt()
    val d = ((this - i) * 10).toInt()
    return "$i.$d"
}

private fun estimatedRankLabel(deltaE: Float): String = when {
    deltaE < 0.5f -> "TOP 1%"
    deltaE < 1.0f -> "TOP 5%"
    deltaE < 1.5f -> "TOP 10%"
    deltaE < 2.0f -> "TOP 20%"
    deltaE < 3.0f -> "TOP 40%"
    deltaE < 4.0f -> "TOP 60%"
    else -> "TOP 80%"
}

private fun estimatedRankDescription(deltaE: Float): String = when {
    deltaE < 0.5f -> "Near human limits"
    deltaE < 1.0f -> "Professional colorist"
    deltaE < 1.5f -> "Trained eye"
    deltaE < 2.0f -> "Designer / photographer"
    deltaE < 3.0f -> "Above average"
    deltaE < 4.0f -> "Average untrained"
    else -> "Just starting out"
}

@OptIn(ExperimentalTime::class)
@Composable
private fun countdownUntil(until: Instant, prefix: String): String {
    val text by produceState(initialValue = "") {
        while (true) {
            val remaining = until - Clock.System.now()
            val totalSeconds = remaining.inWholeSeconds.coerceAtLeast(0)
            value = if (totalSeconds <= 0) {
                ""
            } else {
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                if (hours > 0) "$prefix${hours}h ${minutes}m" else "$prefix${minutes}m"
            }
            delay(60_000L)
        }
    }
    return text
}


