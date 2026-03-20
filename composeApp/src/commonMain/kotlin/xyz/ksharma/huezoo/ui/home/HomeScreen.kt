package xyz.ksharma.huezoo.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.navigation.DailyGame
import xyz.ksharma.huezoo.navigation.ThresholdGame
import xyz.ksharma.huezoo.platform.PlatformOps
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooBodyMedium
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooDisplaySmall
import xyz.ksharma.huezoo.ui.components.HuezooHeadlineLarge
import xyz.ksharma.huezoo.ui.components.HuezooHeadlineSmall
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTitleLarge
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.home.state.DailyCardData
import xyz.ksharma.huezoo.ui.home.state.HomeUiEvent
import xyz.ksharma.huezoo.ui.home.state.HomeUiState
import xyz.ksharma.huezoo.ui.home.state.ThresholdCardData
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.PillShape
import xyz.ksharma.huezoo.ui.theme.rimLight
import xyz.ksharma.huezoo.ui.theme.shapedShadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val STAGGER_DELAY_MS = 80L
private const val CARD_ANIM_DURATION_MS = 300
private const val SLIDE_FRACTION = 5

// Neo-brutalist shelf shadow color and offset
private val ShelfColor = HuezooColors.SurfaceL4
private val ShelfOffset = 4.dp

// Icon box size for compact cards
private val IconBoxSize = 88.dp

// Challenge names — seeded by day-of-year for a consistent daily title
private val CHALLENGE_NAMES = listOf(
    "ULTRAVIOLET BURST", "CRIMSON SURGE", "AZURE DRIFT", "MAGENTA STORM",
    "COBALT PULSE", "EMERALD WAVE", "SOLAR FLARE", "NEON BREACH",
    "VOID ECHO", "AMBER SHIFT", "CORAL STRIKE", "INDIGO TIDE",
    "SCARLET HAZE", "TEAL PHANTOM", "GOLD NOVA", "CERULEAN PEAK",
    "MAUVE IMPACT", "JADE SIGNAL", "RUBY FLASH", "CYAN PROTOCOL",
    "VIOLET APEX", "BRONZE ECHO", "TITANIUM RAY", "ONYX WAVE",
    "PRISM SURGE", "INFRA PULSE", "ULTRA DRIFT", "PLASMA STRIKE",
    "QUARTZ BLOOM", "OMEGA FLASH", "DELTA SURGE", "HELIOS BURST",
)

@OptIn(ExperimentalTime::class)
@Composable
fun HomeScreen(
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val platformOps: PlatformOps = koinInject()

    LifecycleResumeEffect(Unit) {
        viewModel.onUiEvent(HomeUiEvent.ScreenResumed)
        onPauseOrDispose { }
    }

    AmbientGlowBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            HuezooTopBar(
                currencyAmount = (uiState as? HomeUiState.Ready)?.totalGems,
            )

            when (val state = uiState) {
                HomeUiState.Loading -> Unit
                is HomeUiState.Ready -> ReadyContent(
                    state = state,
                    onThresholdTap = { onNavigate(ThresholdGame) },
                    onDailyTap = { onNavigate(DailyGame) },
                    showDebugReset = platformOps.isDebugBuild,
                    onDebugReset = { viewModel.onUiEvent(HomeUiEvent.DebugResetTapped) },
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ReadyContent(
    state: HomeUiState.Ready,
    onThresholdTap: () -> Unit,
    onDailyTap: () -> Unit,
    modifier: Modifier = Modifier,
    showDebugReset: Boolean = false,
    onDebugReset: () -> Unit = {},
) {
    // Derive daily challenge name from today's day-of-year
    val challengeName = remember {
        val dayOfYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfYear
        CHALLENGE_NAMES[dayOfYear % CHALLENGE_NAMES.size]
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HuezooSpacing.md),
    ) {
        Spacer(Modifier.height(HuezooSpacing.md))

        // ── Stats summary row ────────────────────────────────────────────────
        StatsRow(
            totalGems = state.totalGems,
            streak = state.streak,
            rank = state.rank,
        )

        Spacer(Modifier.height(HuezooSpacing.lg))

        // ── Threshold hero card ──────────────────────────────────────────────
        StaggeredCard(index = 0) {
            ThresholdHeroCard(
                data = state.threshold,
                playerLevel = state.playerLevel,
                totalGems = state.totalGems,
                onClick = onThresholdTap,
            )
        }

        Spacer(Modifier.height(HuezooSpacing.lg))

        // ── Daily + Leaderboard side-by-side ─────────────────────────────────
        StaggeredCard(index = 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
            ) {
                DailyCompactCard(
                    data = state.daily,
                    challengeName = challengeName,
                    onClick = onDailyTap,
                    modifier = Modifier.weight(1f),
                )
                LeaderboardCompactCard(
                    rank = state.rank,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(HuezooSpacing.lg))

        // ── ΔE info card (always present, expand/collapse) ───────────────────
        DeltaEInfoCard()

        if (showDebugReset) {
            Spacer(Modifier.height(HuezooSpacing.xl))
            HuezooButton(
                text = "DEBUG: RESET ALL",
                onClick = onDebugReset,
                variant = HuezooButtonVariant.GhostDanger,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(HuezooSpacing.xl))
    }
}

// ── Stats summary row ─────────────────────────────────────────────────────────

/**
 * Asymmetric stats row matching the Stitch design:
 * - Left: large gems panel with cyan left-border
 * - Right: STREAK + RANK stat boxes with top-border accents
 */
@Composable
private fun StatsRow(
    totalGems: Int,
    streak: Int,
    rank: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
    ) {
        // Gems panel — left cyan border, neo-brutal shadow
        Box(
            modifier = Modifier
                .weight(1f)
                .shapedShadow(RectangleShape, ShelfColor, ShelfOffset, ShelfOffset)
                .background(HuezooColors.SurfaceL2)
                .drawBehind {
                    drawRect(
                        color = HuezooColors.AccentCyan,
                        topLeft = Offset(0f, 0f),
                        size = Size(4.dp.toPx(), size.height),
                    )
                }
                .padding(start = HuezooSpacing.md, top = HuezooSpacing.md, end = HuezooSpacing.md, bottom = HuezooSpacing.md),
        ) {
            Column {
                HuezooLabelSmall(
                    text = "CURRENT INVENTORY",
                    color = HuezooColors.TextDisabled,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    HuezooHeadlineLarge(
                        text = formatGems(totalGems),
                        color = HuezooColors.TextPrimary,
                    )
                    Spacer(Modifier.width(HuezooSpacing.xs))
                    HuezooLabelSmall(
                        text = "GEMS",
                        color = HuezooColors.AccentCyan,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }

        // Streak + Rank boxes
        Column(verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
            StatBox(
                label = "STREAK",
                value = "$streak DAYS",
                accentColor = HuezooColors.AccentMagenta,
            )
            StatBox(
                label = "RANK",
                value = rank?.let { "#$it" } ?: "—",
                accentColor = HuezooColors.AccentYellow,
            )
        }
    }
}

/** Single stat box with top-border accent. */
@Composable
private fun StatBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shapedShadow(RectangleShape, ShelfColor, ShelfOffset, ShelfOffset)
            .background(HuezooColors.SurfaceL0)
            .drawBehind {
                drawRect(
                    color = accentColor.copy(alpha = 0.5f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 2.dp.toPx()),
                )
            }
            .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.sm),
    ) {
        Column {
            HuezooLabelSmall(
                text = label,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
            )
            HuezooHeadlineSmall(
                text = value,
                color = HuezooColors.TextPrimary,
            )
        }
    }
}

// ── Threshold hero card ───────────────────────────────────────────────────────

/**
 * Full-width hero card for The Threshold — neo-brutalist, large, with pulsing
 * "ACTIVE MISSION" indicator, level progress bar, and CTA button.
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun ThresholdHeroCard(
    data: ThresholdCardData,
    playerLevel: PlayerLevel,
    totalGems: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val triesText = when {
        data.isBlocked -> "OUT OF TRIES"
        else -> "${data.attemptsRemaining}/${data.maxAttempts} TRIES REMAINING"
    }
    val countdown = data.nextResetAt?.let { countdownUntil(it, prefix = "Resets in ") }
    val enabled = !data.isBlocked

    // Pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    // Level progress
    val progressFraction = levelProgressFraction(playerLevel, totalGems)
    val nextLevelName = nextLevelName(playerLevel)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shapedShadow(RectangleShape, ShelfColor, ShelfOffset, ShelfOffset)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        HuezooColors.GameThreshold.copy(alpha = 0.08f),
                        HuezooColors.SurfaceL2,
                    ),
                ),
            )
            .rimLight(cornerRadius = 0.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        // Right-side gradient illustration placeholder
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(140.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            HuezooColors.GameThreshold.copy(alpha = if (enabled) 0.18f else 0.07f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HuezooSpacing.lg),
        ) {
            // Active mission indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(
                        color = if (enabled) HuezooColors.AccentCyan.copy(alpha = pulseAlpha) else HuezooColors.TextDisabled,
                        radius = size.minDimension / 2f,
                    )
                }
                Spacer(Modifier.width(HuezooSpacing.sm))
                HuezooLabelSmall(
                    text = if (enabled) "ACTIVE MISSION" else "MISSION LOCKED",
                    color = if (enabled) HuezooColors.AccentCyan else HuezooColors.TextDisabled,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(Modifier.height(HuezooSpacing.md))

            // Hero title
            HuezooTitleLarge(
                text = "THE\nTHRESHOLD",
                color = if (enabled) HuezooColors.TextPrimary else HuezooColors.TextDisabled,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(Modifier.height(HuezooSpacing.sm))

            HuezooBodyMedium(
                text = "Analyze chromatic anomalies. Detect the odd color out — precision test for the elite observer.",
                color = HuezooColors.TextSecondary,
                maxLines = 3,
            )

            Spacer(Modifier.height(HuezooSpacing.lg))

            // CTA + tries info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
            ) {
                HuezooButton(
                    text = if (enabled) "ENTER SIMULATION" else "NO TRIES LEFT",
                    onClick = onClick,
                    enabled = enabled,
                    variant = if (enabled) HuezooButtonVariant.Primary else HuezooButtonVariant.GhostDanger,
                )
                if (countdown != null) {
                    HuezooLabelSmall(
                        text = countdown,
                        color = HuezooColors.TextDisabled,
                    )
                } else {
                    HuezooLabelSmall(
                        text = triesText,
                        color = if (enabled) HuezooColors.GameThreshold else HuezooColors.TextDisabled,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Personal best
            data.personalBestDeltaE?.let { de ->
                Spacer(Modifier.height(HuezooSpacing.sm))
                HuezooLabelSmall(
                    text = "BEST: ΔE ${((de * 10).toInt() / 10.0)}",
                    color = HuezooColors.TextDisabled,
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            // Level progress bar
            LevelProgressBar(
                fraction = progressFraction,
                currentLevel = playerLevel,
                nextLevelName = nextLevelName,
                accentColor = playerLevel.levelColor,
            )
        }
    }
}

@Composable
private fun LevelProgressBar(
    fraction: Float,
    currentLevel: PlayerLevel,
    nextLevelName: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Progress track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(HuezooColors.SurfaceL4),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
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
            if (nextLevelName != null) {
                HuezooLabelSmall(text = nextLevelName, color = HuezooColors.TextDisabled)
            }
        }
    }
}

// ── Compact cards ─────────────────────────────────────────────────────────────

/**
 * Daily Challenge compact card — icon box (magenta) + title + countdown.
 * Matches the Stitch side-by-side card layout.
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun DailyCompactCard(
    data: DailyCardData,
    challengeName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val countdown = data.nextPuzzleAt?.let { countdownUntil(it, prefix = "Next in ") }
    val statusText = when {
        data.isCompletedToday -> countdown ?: "Completed today"
        else -> "Available now"
    }

    CompactCard(
        label = "DAILY CHALLENGE",
        title = challengeName,
        subtitle = statusText,
        accentColor = HuezooColors.AccentMagenta,
        enabled = !data.isCompletedToday,
        onClick = onClick,
        iconContent = { color -> drawMedalStar(color) },
        modifier = modifier,
    )
}

/**
 * Leaderboard compact card — icon box (gold) + global rank info.
 * Rank is stubbed until Firebase is integrated; shows "—" as placeholder.
 */
@Composable
private fun LeaderboardCompactCard(
    rank: Int?,
    modifier: Modifier = Modifier,
) {
    CompactCard(
        label = "GLOBAL LEADERBOARD",
        title = if (rank != null) "TOP ${rank}% WORLDWIDE" else "TOP 5% WORLDWIDE",
        subtitle = "Claim weekly rewards",
        accentColor = HuezooColors.AccentYellow,
        enabled = false, // Leaderboard not yet implemented — tappable once Firebase is wired
        onClick = {},
        iconContent = { color -> drawLeaderboardBars(color) },
        modifier = modifier,
    )
}

/**
 * Base layout for a compact card: [iconContent] box on the left, text on the right.
 * Neo-brutalist style: sharp corners, top-border accent, shelf shadow.
 */
@Composable
private fun CompactCard(
    label: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    iconContent: DrawScope.(Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tintColor = if (enabled) accentColor else accentColor.copy(alpha = 0.4f)

    Row(
        modifier = modifier
            .shapedShadow(RectangleShape, ShelfColor, ShelfOffset, ShelfOffset)
            .background(HuezooColors.SurfaceL2)
            .drawBehind {
                // Top accent border
                drawRect(
                    color = tintColor.copy(alpha = 0.6f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 2.dp.toPx()),
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(HuezooSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(IconBoxSize)
                .background(HuezooColors.SurfaceL0)
                .rimLight(cornerRadius = 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(40.dp)) {
                iconContent(tintColor)
            }
        }

        // Text
        Column(modifier = Modifier.weight(1f)) {
            HuezooLabelSmall(
                text = label,
                color = tintColor,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(2.dp))
            HuezooTitleLarge(
                text = title,
                color = if (enabled) HuezooColors.TextPrimary else HuezooColors.TextSecondary,
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

// ── Icon drawers ──────────────────────────────────────────────────────────────

/** 5-pointed star (military medal approximation). Drawn via Canvas. */
private fun DrawScope.drawMedalStar(color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outerR = size.minDimension * 0.48f
    val innerR = outerR * 0.42f
    val path = Path()
    for (i in 0..9) {
        val angle = (i * 36.0 - 90.0) * PI / 180.0
        val r = if (i % 2 == 0) outerR else innerR
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}

/** Three ascending bars (leaderboard/bar-chart icon). Drawn via Canvas. */
private fun DrawScope.drawLeaderboardBars(color: Color) {
    val barW = size.width * 0.22f
    val gap = size.width * 0.07f
    val totalW = barW * 3 + gap * 2
    val startX = (size.width - totalW) / 2f
    val heights = listOf(0.45f, 0.70f, 1.0f)
    heights.forEachIndexed { i, h ->
        val barH = size.height * h
        drawRect(
            color = color,
            topLeft = Offset(startX + i * (barW + gap), size.height - barH),
            size = Size(barW, barH),
        )
    }
}

// ── ΔE info card ─────────────────────────────────────────────────────────────

private val DeltaECardShape = RoundedCornerShape(HuezooSize.CornerCard)

@Composable
private fun DeltaEInfoCard(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "chevron",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HuezooColors.SurfaceL2, DeltaECardShape)
            .border(1.5.dp, HuezooColors.AccentCyan.copy(alpha = 0.28f), DeltaECardShape)
            .clip(DeltaECardShape),
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
            HuezooLabelSmall(
                text = "WHAT IS ΔE?",
                color = HuezooColors.AccentCyan,
                fontWeight = FontWeight.ExtraBold,
            )
            Canvas(
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
            ) {
                val sw = 2.5.dp.toPx()
                val w = size.width; val h = size.height
                drawLine(HuezooColors.AccentCyan.copy(alpha = 0.7f), Offset(w * 0.15f, h * 0.38f), Offset(w * 0.50f, h * 0.65f), sw, StrokeCap.Round)
                drawLine(HuezooColors.AccentCyan.copy(alpha = 0.7f), Offset(w * 0.50f, h * 0.65f), Offset(w * 0.85f, h * 0.38f), sw, StrokeCap.Round)
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)),
        ) {
            Column(modifier = Modifier.padding(start = HuezooSpacing.md, end = HuezooSpacing.md, bottom = HuezooSpacing.md)) {
                HuezooBodyMedium(
                    text = "ΔE measures color difference. Lower = colors are more similar = harder to spot the odd one out.",
                    color = HuezooColors.TextSecondary,
                )
                Spacer(Modifier.height(HuezooSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.xs),
                ) {
                    listOf("≥5 EASY", "2–5 MED", "1–2 HARD", "<1 ELITE").forEach { tier ->
                        HuezooLabelSmall(
                            text = tier,
                            color = HuezooColors.AccentCyan.copy(alpha = 0.75f),
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

// ── Stagger animation ─────────────────────────────────────────────────────────

@Composable
private fun StaggeredCard(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * STAGGER_DELAY_MS)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(CARD_ANIM_DURATION_MS)) +
            slideInVertically(tween(CARD_ANIM_DURATION_MS)) { it / SLIDE_FRACTION },
    ) {
        content()
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatGems(gems: Int): String = when {
    gems >= 1_000 -> "${gems / 1_000},${(gems % 1_000).toString().padStart(3, '0')}"
    else -> "$gems"
}

private fun levelProgressFraction(level: PlayerLevel, totalGems: Int): Float {
    val levels = PlayerLevel.entries
    val nextLevel = levels.getOrNull(level.ordinal + 1) ?: return 1f
    val range = (nextLevel.minGems - level.minGems).toFloat()
    val progress = (totalGems - level.minGems).toFloat()
    return (progress / range).coerceIn(0f, 1f)
}

private fun nextLevelName(level: PlayerLevel): String? {
    val levels = PlayerLevel.entries
    return levels.getOrNull(level.ordinal + 1)?.displayName
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

// ── Previews ─────────────────────────────────────────────────────────────────

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun HomeReadyPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        ReadyContent(
            state = HomeUiState.Ready(
                threshold = ThresholdCardData(
                    personalBestDeltaE = 1.4f,
                    attemptsRemaining = 3,
                    maxAttempts = 5,
                    isBlocked = false,
                ),
                daily = DailyCardData(isCompletedToday = false, todayScore = null),
                isPaid = false,
                totalGems = 1250,
                playerLevel = PlayerLevel.Rookie,
                streak = 14,
                rank = null,
            ),
            onThresholdTap = {},
            onDailyTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun HomeBlockedPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        ReadyContent(
            state = HomeUiState.Ready(
                threshold = ThresholdCardData(
                    personalBestDeltaE = 0.9f,
                    attemptsRemaining = 0,
                    maxAttempts = 5,
                    isBlocked = true,
                ),
                daily = DailyCardData(isCompletedToday = true, todayScore = 740f),
                isPaid = true,
                totalGems = 2450,
                playerLevel = PlayerLevel.Skilled,
                streak = 7,
                rank = 412,
            ),
            onThresholdTap = {},
            onDailyTap = {},
        )
    }
}
