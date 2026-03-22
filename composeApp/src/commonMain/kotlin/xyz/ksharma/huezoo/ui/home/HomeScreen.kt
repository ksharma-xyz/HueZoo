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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import xyz.ksharma.huezoo.ui.components.HuezooDisplayMedium
import xyz.ksharma.huezoo.ui.components.HuezooHeadlineSmall
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTitleLarge
import xyz.ksharma.huezoo.ui.components.HuezooTitleSmall
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.LevelsProgressSheet
import xyz.ksharma.huezoo.ui.components.PerceptionTiersSheet
import xyz.ksharma.huezoo.ui.home.state.DailyCardData
import xyz.ksharma.huezoo.ui.home.state.HomeUiEvent
import xyz.ksharma.huezoo.ui.home.state.HomeUiState
import xyz.ksharma.huezoo.ui.home.state.ThresholdCardData
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
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

private val ShelfColor = HuezooColors.SurfaceL4
private val ShelfOffset = 4.dp
private val IconBoxSize = 80.dp
private val CyanAccentBarWidth = 6.dp

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
    onSettingsTap: () -> Unit,
    onLeaderboardTap: () -> Unit,
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
        when (val state = uiState) {
            HomeUiState.Loading -> Unit
            is HomeUiState.Ready -> ReadyContent(
                state = state,
                onThresholdTap = { onNavigate(ThresholdGame) },
                onDailyTap = { onNavigate(DailyGame) },
                onSettingsTap = onSettingsTap,
                onLeaderboardTap = onLeaderboardTap,
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ReadyContent(
    state: HomeUiState.Ready,
    onThresholdTap: () -> Unit,
    onDailyTap: () -> Unit,
    onSettingsTap: () -> Unit,
    onLeaderboardTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLevelsSheet by remember { mutableStateOf(false) }
    var showPerceptionSheet by remember { mutableStateOf(false) }

    if (showLevelsSheet) {
        LevelsProgressSheet(
            currentGems = state.totalGems,
            onDismiss = { showLevelsSheet = false },
        )
    }
    if (showPerceptionSheet) {
        PerceptionTiersSheet(
            personalBestDeltaE = state.threshold.personalBestDeltaE,
            onDismiss = { showPerceptionSheet = false },
        )
    }
    val challengeName = remember {
        val dayOfYear =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfYear
        CHALLENGE_NAMES[dayOfYear % CHALLENGE_NAMES.size]
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // TopBar scrolls with content; its windowInsetsTopHeight spacer provides status-bar padding
        HuezooTopBar(onSettingsClick = onSettingsTap)

        // Padded content column — TopBar stays full-width above
        Column(modifier = Modifier.padding(horizontal = HuezooSpacing.md)) {
            Spacer(Modifier.height(HuezooSpacing.md))

            // Greeting — tapping name navigates to Settings to set/update it
            HuezooLabelSmall(
                text = "WELCOME,",
                color = HuezooColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            HuezooHeadlineSmall(
                text = if (state.userName != null) state.userName.uppercase() else "AGENT",
                color = LocalPlayerAccentColor.current,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSettingsTap,
                ),
            )

            Spacer(Modifier.height(HuezooSpacing.md))

            StaggeredCard(index = 0) {
                StatsSection(
                    totalGems = state.totalGems,
                    streak = state.streak,
                    rank = state.rank,
                    onGemsClick = { showLevelsSheet = true },
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            StaggeredCard(index = 1) {
                PlayerDeltaECard(
                    bestDeltaE = state.threshold.personalBestDeltaE,
                    onClick = { showPerceptionSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            LevelProgressBar(
                fraction = levelProgressFraction(state.playerLevel, state.totalGems),
                currentLevel = state.playerLevel,
                totalGems = state.totalGems,
                accentColor = state.playerLevel.levelColor,
                onClick = { showLevelsSheet = true },
                modifier = Modifier.padding(horizontal = HuezooSpacing.xs),
            )

            Spacer(Modifier.height(HuezooSpacing.sm))

            StaggeredCard(index = 2) {
                ThresholdHeroCard(
                    data = state.threshold,
                    isPaid = state.isPaid,
                    onEnterGame = onThresholdTap,
                    onUpgradeTap = onSettingsTap,
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            StaggeredCard(index = 3) {
                DailyCompactCard(
                    data = state.daily,
                    challengeName = challengeName,
                    onClick = onDailyTap,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(HuezooSpacing.md))

            StaggeredCard(index = 4) {
                LeaderboardCompactCard(
                    personalBestDeltaE = state.threshold.personalBestDeltaE,
                    onClick = { onLeaderboardTap() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            DeltaEInfoCard()

            Spacer(Modifier.height(HuezooSpacing.xxl).navigationBarsPadding())
        }
    }
}

// ── Stats section ─────────────────────────────────────────────────────────────

/**
 * Stats section layout (mobile-first, matches Stitch):
 *
 * CURRENT INVENTORY  ← label above, outside the box
 * ┌─────────────────────────────┐
 * │▌ 1,250  GEMS                │  ← cyan left accent bar + neo-brutal shadow
 * └─────────────────────────────┘
 * ┌──────────────┐ ┌────────────┐
 * │ STREAK       │ │ RANK       │  ← equal-width stat boxes
 * │ 0 DAYS       │ │ —          │
 * └──────────────┘ └────────────┘
 */
@Composable
private fun StatsSection(
    totalGems: Int,
    streak: Int,
    rank: Int?,
    onGemsClick: () -> Unit,
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

        // Gems panel — cyan left accent bar, neo-brutal right+bottom shadow
        // height(IntrinsicSize.Min) lets the accent bar match content height via fillMaxHeight()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .shapedShadow(RectangleShape, ShelfColor, ShelfOffset, ShelfOffset),
        ) {
            // Level-color accent bar
            Box(
                modifier = Modifier
                    .width(CyanAccentBarWidth)
                    .fillMaxHeight()
                    .background(LocalPlayerAccentColor.current),
            )
            // Gems content — tappable to open Levels & Progress sheet
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
                // Gem spill illustration — behind text
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

        // Streak + Rank side-by-side, equal width
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = HuezooSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            StatBox(
                label = "STREAK",
                value = "$streak DAYS",
                accentColor = HuezooColors.AccentMagenta,
                modifier = Modifier.weight(1f),
            )
            StatBox(
                label = "RANK",
                value = rank?.let { "#$it" } ?: "—",
                accentColor = HuezooColors.AccentYellow,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

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
            HuezooHeadlineSmall(
                text = value,
                color = HuezooColors.TextPrimary,
            )
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
private fun ThresholdHeroCard(
    data: ThresholdCardData,
    isPaid: Boolean,
    onEnterGame: () -> Unit,
    onUpgradeTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val triesText = when {
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
        // Tactical scanner illustration — fills the full card, drawn behind content
        ThresholdScannerIllustration(
            enabled = enabled,
            modifier = Modifier.matchParentSize(),
        )

        Column(modifier = Modifier.fillMaxWidth().padding(HuezooSpacing.lg)) {
            // Active mission dot
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

            // Tries info — start-aligned, full width
            HuezooLabelSmall(
                text = countdown ?: triesText,
                color = if (enabled) HuezooColors.GameThreshold else HuezooColors.TextDisabled,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )

            // Personal best inline
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
                // Blocked — show disabled state + upgrade/ad options
                HuezooButton(
                    text = "NO TRIES LEFT",
                    onClick = {},
                    enabled = false,
                    variant = HuezooButtonVariant.GhostDanger,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isPaid) {
                    Spacer(Modifier.height(HuezooSpacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
                    ) {
                        HuezooButton(
                            text = "GET FULL ACCESS",
                            onClick = onUpgradeTap,
                            variant = HuezooButtonVariant.Primary,
                            modifier = Modifier.weight(1f),
                        )
                        HuezooButton(
                            text = "WATCH AD",
                            onClick = { /* TODO: rewarded ad integration */ },
                            variant = HuezooButtonVariant.Ghost,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

// ── Threshold scanner illustration ───────────────────────────────────────────

/**
 * Tactical scanner drawn entirely from lines — GI Joe / robot sensor array aesthetic.
 *
 * Elements:
 *  • Dot grid background (holographic HUD feel)
 *  • 5 concentric partial arcs anchored at the right edge (range rings)
 *  • Tick marks on each arc (major every 4th, minor otherwise)
 *  • Horizontal + vertical crosshair lines with a gap around the focal point
 *  • Radar sweep arm + a ghost trailing arm
 *  • Center focal diamond + cyan dot
 *  • Data blips (small squares) scattered along the outer arcs
 *  • Military corner bracket markers
 *
 * All elements drawn in [HuezooColors.GameThreshold] (indigo-violet) with [HuezooColors.AccentCyan]
 * accents. Alpha drops off toward the left edge, keeping text readable.
 */
@Composable
private fun ThresholdScannerIllustration(
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val baseColor = HuezooColors.GameThreshold
    val accentColor = LocalPlayerAccentColor.current
    val dimFactor = if (enabled) 1f else 0.35f

    Canvas(modifier = modifier) {
        val sw = 1.dp.toPx()
        val w = size.width
        val h = size.height

        // Reticle anchor: slightly off-screen right, upper-third of card
        val cx = w + 16.dp.toPx()
        val cy = h * 0.28f

        // ── 1. Dot grid ──────────────────────────────────────────────────────
        val dotStep = 22.dp.toPx()
        val dotR = 1.2.dp.toPx()
        var gx = 0f
        while (gx <= w) {
            var gy = 0f
            while (gy <= h) {
                // Fade dots on the left quarter so text stays readable
                val fadeAlpha = ((gx / w - 0.25f) / 0.75f).coerceIn(0f, 1f)
                drawCircle(
                    color = baseColor.copy(alpha = 0.07f * fadeAlpha * dimFactor),
                    radius = dotR,
                    center = Offset(gx, gy),
                )
                gy += dotStep
            }
            gx += dotStep
        }

        // ── 2. Concentric range arcs ─────────────────────────────────────────
        val arcRadii = listOf(70.0, 115.0, 165.0, 222.0, 285.0).map { it.dp.toPx() }
        val arcStart = 140f // degrees — leftward-facing sweep
        val arcSweep = 185f

        arcRadii.forEachIndexed { i, r ->
            val arcAlpha = (0.22f - i * 0.025f) * dimFactor
            drawArc(
                color = baseColor.copy(arcAlpha),
                startAngle = arcStart,
                sweepAngle = arcSweep,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = sw),
            )

            // Tick marks
            val numTicks = 9 + i * 3
            for (t in 0..numTicks) {
                val angleDeg = arcStart + t * (arcSweep / numTicks.toFloat())
                val rad = angleDeg * PI / 180.0
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                val isMajor = t % 4 == 0
                val tickLen = (if (isMajor) 5.5.dp else 2.5.dp).toPx()
                val tickAlpha = (if (isMajor) arcAlpha * 1.8f else arcAlpha).coerceAtMost(0.55f)
                drawLine(
                    color = baseColor.copy(tickAlpha),
                    start = Offset(cx + (r - tickLen) * cosA, cy + (r - tickLen) * sinA),
                    end = Offset(cx + r * cosA, cy + r * sinA),
                    strokeWidth = sw,
                )
            }
        }

        // ── 3. Crosshair lines ────────────────────────────────────────────────
        val crossAlpha = 0.13f * dimFactor
        val gapR = 40.dp.toPx()
        // Horizontal arm going left
        drawLine(baseColor.copy(crossAlpha), Offset(0f, cy), Offset(cx - gapR, cy), sw)
        // Vertical arm going down
        drawLine(
            baseColor.copy(crossAlpha),
            Offset(cx, cy + gapR),
            Offset(cx, h + 20.dp.toPx()),
            sw,
        )
        // Vertical arm going up
        drawLine(baseColor.copy(crossAlpha), Offset(cx, 0f), Offset(cx, cy - gapR), sw)

        // ── 4. Radar sweep arm + ghost ────────────────────────────────────────
        val sweepDeg = 212.0
        val sweepRad = sweepDeg * PI / 180.0
        val sweepLen = arcRadii.last()
        drawLine(
            color = baseColor.copy(0.30f * dimFactor),
            start = Offset(cx, cy),
            end = Offset(
                (cx + sweepLen * cos(sweepRad)).toFloat(),
                (cy + sweepLen * sin(sweepRad)).toFloat(),
            ),
            strokeWidth = sw * 1.6f,
        )
        val ghostRad = (sweepDeg - 14.0) * PI / 180.0
        drawLine(
            color = baseColor.copy(0.10f * dimFactor),
            start = Offset(cx, cy),
            end = Offset(
                (cx + sweepLen * cos(ghostRad)).toFloat(),
                (cy + sweepLen * sin(ghostRad)).toFloat(),
            ),
            strokeWidth = sw,
        )

        // ── 5. Center focal diamond + cyan dot ────────────────────────────────
        val d = 9.dp.toPx()
        val diamond = Path().apply {
            moveTo(cx, cy - d)
            lineTo(cx + d, cy)
            lineTo(cx, cy + d)
            lineTo(cx - d, cy)
            close()
        }
        drawPath(diamond, baseColor.copy(0.45f * dimFactor), style = Stroke(sw * 1.5f))
        drawCircle(accentColor.copy(0.80f * dimFactor), 2.5.dp.toPx(), Offset(cx, cy))

        // ── 6. Data blips on arcs ─────────────────────────────────────────────
        data class Blip(val arcIdx: Int, val angleDeg: Float)

        val blips = listOf(
            Blip(2, 148f), Blip(2, 182f), Blip(2, 225f), Blip(2, 275f), Blip(2, 318f),
            Blip(3, 155f), Blip(3, 200f), Blip(3, 260f), Blip(3, 310f),
            Blip(4, 170f), Blip(4, 240f), Blip(4, 290f),
        )
        blips.forEach { (arcIdx, angleDeg) ->
            val r = arcRadii[arcIdx]
            val rad = angleDeg * PI / 180.0
            val bx = (cx + r * cos(rad)).toFloat()
            val by = (cy + r * sin(rad)).toFloat()
            if (bx < w && by >= 0f && by <= h) {
                val bSz = 4.dp.toPx()
                val isAccent = angleDeg in 180f..200f || angleDeg in 300f..320f
                val blipColor = if (isAccent) accentColor else baseColor
                val blipAlpha = if (isAccent) 0.60f * dimFactor else 0.35f * dimFactor
                drawRect(
                    blipColor.copy(blipAlpha),
                    Offset(bx - bSz / 2f, by - bSz / 2f),
                    Size(bSz, bSz),
                )
            }
        }

        // ── 7. Military corner brackets ───────────────────────────────────────
        val bLen = 12.dp.toPx()
        val bAlpha = 0.22f * dimFactor
        val bSw = sw * 1.8f

        data class Corner(val x: Float, val y: Float, val dx: Float, val dy: Float)
        listOf(
            Corner(0f, 0f, 1f, 1f), // top-left
            Corner(w, 0f, -1f, 1f), // top-right
            Corner(0f, h, 1f, -1f), // bottom-left
            Corner(w, h, -1f, -1f), // bottom-right
        ).forEach { (x, y, dx, dy) ->
            drawLine(baseColor.copy(bAlpha), Offset(x, y), Offset(x + dx * bLen, y), bSw)
            drawLine(baseColor.copy(bAlpha), Offset(x, y), Offset(x, y + dy * bLen), bSw)
        }

        // ── 8. Horizontal data-scan line at 60% height ───────────────────────
        val scanY = h * 0.62f
        drawLine(
            color = accentColor.copy(0.08f * dimFactor),
            start = Offset(w * 0.35f, scanY),
            end = Offset(w, scanY),
            strokeWidth = sw,
        )
        // Tick on scan line every 18dp
        var tx = w * 0.40f
        while (tx <= w - 4.dp.toPx()) {
            drawLine(
                accentColor.copy(0.12f * dimFactor),
                Offset(tx, scanY - 3.dp.toPx()),
                Offset(tx, scanY + 3.dp.toPx()),
                sw,
            )
            tx += 18.dp.toPx()
        }
    }
}

@Composable
private fun LevelProgressBar(
    fraction: Float,
    currentLevel: PlayerLevel,
    totalGems: Int,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextLevel = PlayerLevel.entries.getOrNull(currentLevel.ordinal + 1)
    val rightLabel = nextLevel?.let { "${formatGems(totalGems)} / ${formatGems(it.minGems)}" }

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
            if (rightLabel != null) {
                HuezooLabelSmall(text = rightLabel, color = HuezooColors.TextDisabled)
            }
        }
    }
}

// ── Compact cards ─────────────────────────────────────────────────────────────

/**
 * Daily Challenge compact card — full width, stacked below Threshold.
 * Clickable when daily is not yet completed today.
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
    val bestText = data.personalBestRounds?.let { "BEST: $it / 6 ROUNDS" }
    val subtitleText = when {
        data.isCompletedToday -> countdown ?: "Completed today"
        bestText != null -> bestText
        else -> "Available now"
    }

    CompactCard(
        label = "DAILY CHALLENGE",
        title = challengeName,
        subtitle = subtitleText,
        accentColor = HuezooColors.AccentMagenta,
        enabled = !data.isCompletedToday,
        onClick = onClick,
        iconDraw = { color -> drawMedalStar(color) },
        modifier = modifier,
    )
}

/**
 * Global Leaderboard compact card — shows player's estimated rank tier.
 */
@Composable
private fun LeaderboardCompactCard(
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

/**
 * Compact card matching the Stitch design:
 * - Left: fixed-size icon box with neon top-left rim + icon drawn via Canvas
 * - Right: label (small, accent) / title (large headline) / subtitle (small, secondary)
 * - Neo-brutalist top-border accent + shelf shadow
 *
 * Colors always at full brightness — clickable state only gates the onClick action.
 */
@Composable
private fun CompactCard(
    label: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    iconDraw: DrawScope.(Color) -> Unit,
    modifier: Modifier = Modifier,
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
        // Icon box — fixed size, neon top-left rim drawn over SurfaceL0 background
        Box(
            modifier = Modifier
                .size(IconBoxSize)
                .background(HuezooColors.SurfaceL0)
                .drawBehind {
                    val stroke = 1.5.dp.toPx()
                    // top rim
                    drawLine(
                        accentColor.copy(alpha = 0.5f),
                        Offset(0f, stroke / 2f),
                        Offset(size.width, stroke / 2f),
                        stroke,
                    )
                    // left rim
                    drawLine(
                        accentColor.copy(alpha = 0.5f),
                        Offset(stroke / 2f, 0f),
                        Offset(stroke / 2f, size.height),
                        stroke,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(36.dp)) {
                iconDraw(accentColor)
            }
        }

        // Text content
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

// ── Player ΔE hype card ───────────────────────────────────────────────────────

/**
 * Hero-style card that anchors the entire app around the ΔE concept.
 *
 * For first-time players: explains what ΔE is and sets the goal.
 * For returning players: displays their personal best and motivates them to go lower.
 */
/**
 * Compact ΔE personal-best card.
 *
 * Layout: left = label + subtitle | right = big ΔE value (or "N/A")
 * Visual: right-side only accent border (3 dp vertical bar), no outer border on other sides.
 */
@Composable
private fun PlayerDeltaECard(
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
            // Right-side accent bar drawn as a custom drawBehind
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
        // Left: label + subtitle
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

        // Right: big value
        androidx.compose.material3.Text(
            text = if (hasPlayed) bestDeltaE!!.fmtHome() else "N/A",
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
            color = if (hasPlayed) accent else HuezooColors.TextDisabled,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = HuezooSpacing.md),
        )
    }
}

private fun Float.fmtHome(): String {
    val i = toInt()
    val d = ((this - i) * 10).toInt()
    return "$i.$d"
}

// ── ΔE info card ─────────────────────────────────────────────────────────────

@Composable
private fun DeltaEInfoCard(modifier: Modifier = Modifier) {
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
                        "ΔE 10 → red vs blue — instantly obvious\n" +
                        "ΔE 3  → clearly different if side by side\n" +
                        "ΔE 1  → just noticeable to a trained eye\n" +
                        "ΔE 0.5 → near the limit of human vision\n\n" +
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

/**
 * A pile of faceted gem diamonds spilling in from the right — decorative illustration
 * for the inventory card. Mirrors the craft level of [ThresholdScannerIllustration].
 *
 * Layout: one large hero gem + several medium/small gems scattered around it,
 * all anchored to the right half so the left-side text stays readable.
 */
@Composable
private fun GemSpillIllustration(modifier: Modifier = Modifier) {
    val color = LocalPlayerAccentColor.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 0.9.dp.toPx()

        // ── Anchor point: right-center, slightly off-screen ─────────────────
        val anchorX = w * 0.78f
        val anchorY = h * 0.5f

        // ── Soft radial glow behind the hero gem ────────────────────────────
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.09f), Color.Transparent),
                center = Offset(anchorX, anchorY),
                radius = h * 1.1f,
            ),
            radius = h * 1.1f,
            center = Offset(anchorX, anchorY),
        )

        // ── Gem definitions: cx, cy (relative to anchor), size, rotation deg ─
        data class GemSpec(val dx: Float, val dy: Float, val size: Float, val rotDeg: Float)

        val specs = listOf(
            GemSpec(0f, 0f, h * 0.72f, 0f), // hero — center
            GemSpec(-h * 0.55f, h * 0.10f, h * 0.42f, -18f), // left-lower
            GemSpec(-h * 0.30f, -h * 0.40f, h * 0.32f, 22f), // upper-left
            GemSpec(h * 0.28f, -h * 0.38f, h * 0.26f, -30f), // upper-right
            GemSpec(h * 0.22f, h * 0.35f, h * 0.22f, 14f), // lower-right
            GemSpec(-h * 0.65f, -h * 0.25f, h * 0.18f, 35f), // far upper-left (tiny)
            GemSpec(h * 0.08f, -h * 0.55f, h * 0.14f, -10f), // top micro
        )

        specs.forEachIndexed { idx, spec ->
            val cx = anchorX + spec.dx
            val cy = anchorY + spec.dy
            val s = spec.size
            // Skip gems that are mostly off-screen left (would overlap text)
            if (cx + s < w * 0.38f) return@forEachIndexed
            val alpha = when (idx) {
                0 -> 0.18f // hero — most visible
                1 -> 0.13f
                2 -> 0.11f
                else -> 0.08f
            }

            // Build gem path centred at origin, then rotate + translate via withTransform
            val rotRad = spec.rotDeg * PI.toFloat() / 180f
            val cosR = cos(rotRad)
            val sinR = sin(rotRad)

            fun rotated(px: Float, py: Float): Offset {
                return Offset(cx + px * cosR - py * sinR, cy + px * sinR + py * cosR)
            }

            // Gem silhouette: crown (upper trapezoid) + pavilion (lower triangle)
            // Points: top, upper-right shoulder, lower-right hip, bottom, lower-left hip, upper-left shoulder
            val top = rotated(0f, -s * 0.5f)
            val upR = rotated(s * 0.40f, -s * 0.14f)
            val loR = rotated(s * 0.28f, s * 0.12f)
            val bot = rotated(0f, s * 0.5f)
            val loL = rotated(-s * 0.28f, s * 0.12f)
            val upL = rotated(-s * 0.40f, -s * 0.14f)

            // Fill
            val gemPath = Path().apply {
                moveTo(top.x, top.y)
                lineTo(upR.x, upR.y)
                lineTo(loR.x, loR.y)
                lineTo(bot.x, bot.y)
                lineTo(loL.x, loL.y)
                lineTo(upL.x, upL.y)
                close()
            }
            drawPath(gemPath, color.copy(alpha = alpha))

            // Outline
            drawPath(gemPath, color.copy(alpha = alpha * 1.5f), style = Stroke(width = sw))

            // Crown facet lines
            val girL = rotated(-s * 0.20f, -s * 0.14f)
            val girR = rotated(s * 0.20f, -s * 0.14f)
            val facetAlpha = (alpha * 1.8f).coerceAtMost(0.45f)
            drawLine(color.copy(facetAlpha), top, girL, sw * 0.8f)
            drawLine(color.copy(facetAlpha), top, girR, sw * 0.8f)
            drawLine(color.copy(facetAlpha), girL, girR, sw * 0.8f)

            // Pavilion keel line (from girdle centre to bottom)
            val girdleCentre = rotated(0f, -s * 0.14f)
            drawLine(color.copy(alpha * 1.4f), girdleCentre, bot, sw * 0.7f)
        }

        // ── Sparkle crosses scattered around the pile ────────────────────────
        data class Sparkle(val x: Float, val y: Float, val r: Float)
        val sparkles = listOf(
            Sparkle(anchorX - h * 0.42f, anchorY - h * 0.58f, h * 0.055f),
            Sparkle(anchorX + h * 0.18f, anchorY - h * 0.50f, h * 0.040f),
            Sparkle(anchorX + h * 0.35f, anchorY + h * 0.30f, h * 0.035f),
            Sparkle(anchorX - h * 0.72f, anchorY + h * 0.20f, h * 0.030f),
            Sparkle(anchorX - h * 0.10f, anchorY + h * 0.52f, h * 0.028f),
        )
        sparkles.forEach { (sx, sy, r) ->
            if (sx < w * 0.42f) return@forEach // stay out of text zone
            val a = 0.20f
            drawLine(color.copy(a), Offset(sx - r, sy), Offset(sx + r, sy), sw * 0.8f)
            drawLine(color.copy(a), Offset(sx, sy - r), Offset(sx, sy + r), sw * 0.8f)
            // Diagonal arms (×) at half length and lower alpha
            val dr = r * 0.55f
            drawLine(color.copy(a * 0.6f), Offset(sx - dr, sy - dr), Offset(sx + dr, sy + dr), sw * 0.6f)
            drawLine(color.copy(a * 0.6f), Offset(sx - dr, sy + dr), Offset(sx + dr, sy - dr), sw * 0.6f)
        }

        // ── Horizontal scan line (matches Threshold card's aesthetic) ────────
        val scanY = h * 0.68f
        drawLine(
            color.copy(alpha = 0.07f),
            Offset(w * 0.45f, scanY),
            Offset(w, scanY),
            sw * 0.8f,
        )
        var tx = w * 0.50f
        while (tx < w - 2.dp.toPx()) {
            drawLine(color.copy(0.10f), Offset(tx, scanY - 2.5.dp.toPx()), Offset(tx, scanY + 2.5.dp.toPx()), sw * 0.7f)
            tx += 16.dp.toPx()
        }
    }
}

private fun formatGems(gems: Int): String = when {
    gems >= 1_000 -> "${gems / 1_000},${(gems % 1_000).toString().padStart(3, '0')}"
    else -> "$gems"
}

private fun levelProgressFraction(level: PlayerLevel, totalGems: Int): Float {
    val next = PlayerLevel.entries.getOrNull(level.ordinal + 1) ?: return 1f
    val range = (next.minGems - level.minGems).toFloat()
    val progress = (totalGems - level.minGems).toFloat()
    return (progress / range).coerceIn(0f, 1f)
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
                daily = DailyCardData(isCompletedToday = false),
                isPaid = false,
                totalGems = 1250,
                playerLevel = PlayerLevel.Rookie,
                streak = 14,
                rank = null,
            ),
            onThresholdTap = {},
            onDailyTap = {},
            onSettingsTap = {},
            onLeaderboardTap = {},
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
                daily = DailyCardData(isCompletedToday = true, personalBestRounds = 5),
                isPaid = true,
                totalGems = 2450,
                playerLevel = PlayerLevel.Sharp,
                streak = 7,
                rank = 412,
            ),
            onThresholdTap = {},
            onDailyTap = {},
            onSettingsTap = {},
            onLeaderboardTap = {},
        )
    }
}
