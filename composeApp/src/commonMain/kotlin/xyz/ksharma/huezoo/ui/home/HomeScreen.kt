package xyz.ksharma.huezoo.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import xyz.ksharma.huezoo.ui.components.GameCard
import xyz.ksharma.huezoo.ui.components.HuezooBodyMedium
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooDisplayMedium
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTitleMedium
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val STAGGER_DELAY_MS = 80L
private const val CARD_ANIM_DURATION_MS = 300
private const val SLIDE_FRACTION = 5

@Composable
fun HomeScreen(
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val platformOps: PlatformOps = koinInject()

    // Re-load every time HomeScreen becomes the active top entry (e.g. returning from a game).
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

@Composable
private fun ReadyContent(
    state: HomeUiState.Ready,
    onThresholdTap: () -> Unit,
    onDailyTap: () -> Unit,
    modifier: Modifier = Modifier,
    showDebugReset: Boolean = false,
    onDebugReset: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(HuezooSpacing.md),
    ) {
        Spacer(Modifier.height(HuezooSpacing.sm))

        HeroStatsRow(
            totalGems = state.totalGems,
            playerLevel = state.playerLevel,
        )

        Spacer(Modifier.height(HuezooSpacing.md))

        DeltaEInfoCard()

        Spacer(Modifier.height(HuezooSpacing.lg))

        StaggeredCard(index = 0) {
            ThresholdCard(data = state.threshold, onClick = onThresholdTap)
        }

        Spacer(Modifier.height(HuezooSpacing.lg))

        StaggeredCard(index = 1) {
            DailyCard(data = state.daily, onClick = onDailyTap)
        }

        if (showDebugReset) {
            Spacer(Modifier.height(HuezooSpacing.xl))
            HuezooButton(
                text = "DEBUG: RESET ALL",
                onClick = onDebugReset,
                variant = HuezooButtonVariant.GhostDanger,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(HuezooSpacing.md))
    }
}

// ── Hero stats row ────────────────────────────────────────────────────────────

/**
 * Level badge — sits directly below the top bar.
 * Gems are shown in HuezooTopBar; this row shows the player's current level.
 */
@Composable
private fun HeroStatsRow(
    totalGems: Int,
    playerLevel: PlayerLevel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(playerLevel.levelColor.copy(alpha = 0.15f), PillShape)
            .border(1.5.dp, playerLevel.levelColor.copy(alpha = 0.6f), PillShape)
            .padding(horizontal = HuezooSize.BadgeHorizontalPad, vertical = 6.dp),
    ) {
        HuezooLabelSmall(
            text = playerLevel.displayName,
            color = playerLevel.levelColor,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

// ── ΔE info card ─────────────────────────────────────────────────────────────

private val DeltaECardShape = RoundedCornerShape(HuezooSize.CornerCard)

/**
 * Always-present expandable card explaining ΔE.
 * Collapsed by default — tap the header to expand/collapse.
 * Never dismissed; state is local to the composition.
 */
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
            .border(1.5.dp, HuezooColors.AccentCyan.copy(alpha = 0.30f), DeltaECardShape)
            .clip(DeltaECardShape),
    ) {
        // Header row — always visible, tappable
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
            // Chevron drawn via Canvas — rotates to ▲ when expanded
            Canvas(
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
            ) {
                drawChevronDown(color = HuezooColors.AccentCyan.copy(alpha = 0.7f))
            }
        }

        // Expandable body
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
                DeltaEScaleRow()
            }
        }
    }
}

/** Mini ΔE difficulty scale — gives new players a quick reference. */
@Composable
private fun DeltaEScaleRow(modifier: Modifier = Modifier) {
    val tiers = listOf(
        "≥5.0" to "EASY",
        "2–5" to "MODERATE",
        "1–2" to "HARD",
        "<1.0" to "EXPERT",
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.xs),
    ) {
        tiers.forEach { (range, label) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HuezooLabelSmall(
                    text = range,
                    color = HuezooColors.AccentCyan,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                HuezooLabelSmall(
                    text = label,
                    color = HuezooColors.TextDisabled,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun DrawScope.drawChevronDown(color: Color) {
    val strokeW = 2.5.dp.toPx()
    val w = size.width
    val h = size.height
    drawLine(color = color, start = Offset(w * 0.15f, h * 0.38f), end = Offset(w * 0.50f, h * 0.65f), strokeWidth = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(color = color, start = Offset(w * 0.50f, h * 0.65f), end = Offset(w * 0.85f, h * 0.38f), strokeWidth = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
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

// ── Game cards ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
@Composable
private fun ThresholdCard(
    data: ThresholdCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val triesText = when {
        data.isBlocked -> "No tries left"
        else -> "${data.attemptsRemaining}/${data.maxAttempts} tries remaining"
    }
    val personalBest = data.personalBestDeltaE?.let { de ->
        val rounded = (de * 10).toInt() / 10.0
        "Best: ΔE $rounded"
    }
    val countdown = data.nextResetAt?.let { countdownUntil(it, prefix = "Resets in ") }

    GameCard(
        title = "The Threshold",
        subtitle = "Tap the odd color out. One miss ends your run.",
        identityColor = HuezooColors.GameThreshold,
        onClick = onClick,
        enabled = !data.isBlocked,
        isHero = true,
        triesText = triesText,
        personalBest = personalBest,
        countdownText = countdown,
        visualContent = { ThresholdIllustration(isBlocked = data.isBlocked) },
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalTime::class)
@Composable
private fun DailyCard(
    data: DailyCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeText = if (data.isCompletedToday) "Done" else null
    val personalBest = data.todayScore?.let { "Score: ${it.toInt()}" }
    val countdown = data.nextPuzzleAt?.let { countdownUntil(it, prefix = "Next puzzle in ") }

    GameCard(
        title = "Daily Challenge",
        subtitle = "6 rounds. One chance. Same for everyone today.",
        identityColor = HuezooColors.GameDaily,
        onClick = onClick,
        enabled = !data.isCompletedToday,
        badgeText = badgeText,
        personalBest = personalBest,
        countdownText = countdown,
        visualContent = { DailyIllustration() },
        modifier = modifier.fillMaxWidth(),
    )
}

// ── Card illustration areas ───────────────────────────────────────────────────

/**
 * Threshold illustration — three swatch tiles with the middle one subtly different,
 * communicating the "spot the odd one out" mechanic at a glance.
 */
@Composable
private fun ThresholdIllustration(isBlocked: Boolean, modifier: Modifier = Modifier) {
    val accentAlpha = if (isBlocked) 0.35f else 1f
    val baseColor = HuezooColors.GameThreshold.copy(alpha = 0.65f * accentAlpha)
    val oddColor = HuezooColors.GameThreshold.copy(alpha = 0.30f * accentAlpha)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        HuezooColors.GameThreshold.copy(alpha = 0.20f * accentAlpha),
                        Color.Transparent,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(width = 168.dp, height = 52.dp)) {
                val gap = 10.dp.toPx()
                val swatchW = (size.width - gap * 2) / 3f
                val corner = CornerRadius(10.dp.toPx())

                drawRoundRect(color = baseColor, topLeft = Offset(0f, 0f), size = Size(swatchW, size.height), cornerRadius = corner)
                drawRoundRect(color = oddColor, topLeft = Offset(swatchW + gap, 0f), size = Size(swatchW, size.height), cornerRadius = corner)
                drawRoundRect(color = baseColor, topLeft = Offset((swatchW + gap) * 2f, 0f), size = Size(swatchW, size.height), cornerRadius = corner)
            }
            Spacer(Modifier.height(HuezooSpacing.sm))
            HuezooLabelSmall(
                text = "DETECT THE ODD COLOR OUT",
                color = HuezooColors.GameThreshold.copy(alpha = 0.55f * accentAlpha),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Daily illustration — today's date shown large, evoking a "daily puzzle" feel.
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun DailyIllustration(modifier: Modifier = Modifier) {
    val (day, month) = remember {
        val local = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        local.dayOfMonth.toString() to local.month.name.take(3).uppercase()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        HuezooColors.GameDaily.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HuezooLabelSmall(
                text = month,
                color = HuezooColors.GameDaily.copy(alpha = 0.75f),
                fontWeight = FontWeight.ExtraBold,
            )
            HuezooDisplayMedium(
                text = day,
                color = HuezooColors.GameDaily,
                fontWeight = FontWeight.ExtraBold,
            )
            HuezooLabelSmall(
                text = "SAME FOR EVERYONE",
                color = HuezooColors.GameDaily.copy(alpha = 0.5f),
            )
        }
    }
}

// ── Countdown ─────────────────────────────────────────────────────────────────

/**
 * Returns a live-updating countdown string (e.g. "Resets in 2h 14m") that
 * ticks every minute until [until] is reached.
 */
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
                daily = DailyCardData(
                    isCompletedToday = false,
                    todayScore = null,
                ),
                isPaid = false,
                totalGems = 128,
                playerLevel = PlayerLevel.Rookie,
            ),
            onThresholdTap = {},
            onDailyTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun HomeBlockedAndCompletedPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        ReadyContent(
            state = HomeUiState.Ready(
                threshold = ThresholdCardData(
                    personalBestDeltaE = 0.9f,
                    attemptsRemaining = 0,
                    maxAttempts = 5,
                    isBlocked = true,
                ),
                daily = DailyCardData(
                    isCompletedToday = true,
                    todayScore = 740f,
                ),
                isPaid = true,
                totalGems = 2450,
                playerLevel = PlayerLevel.Skilled,
            ),
            onThresholdTap = {},
            onDailyTap = {},
        )
    }
}
