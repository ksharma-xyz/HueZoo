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
import xyz.ksharma.huezoo.ui.home.state.DailyCardData
import xyz.ksharma.huezoo.ui.home.state.HomeUiEvent
import xyz.ksharma.huezoo.ui.home.state.HomeUiState
import xyz.ksharma.huezoo.ui.home.state.ThresholdCardData
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSize
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
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
                showDebugReset = platformOps.isDebugBuild,
                onDebugReset = { viewModel.onUiEvent(HomeUiEvent.DebugResetTapped) },
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
    modifier: Modifier = Modifier,
    showDebugReset: Boolean = false,
    onDebugReset: () -> Unit = {},
) {
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
        HuezooTopBar()

        // Padded content column — TopBar stays full-width above
        Column(modifier = Modifier.padding(horizontal = HuezooSpacing.md)) {
            Spacer(Modifier.height(HuezooSpacing.md))

            StaggeredCard(index = 0) {
                StatsSection(
                    totalGems = state.totalGems,
                    streak = state.streak,
                    rank = state.rank,
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            StaggeredCard(index = 1) {
                ThresholdHeroCard(
                    data = state.threshold,
                    playerLevel = state.playerLevel,
                    totalGems = state.totalGems,
                    onEnterGame = onThresholdTap,
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

            StaggeredCard(index = 2) {
                DailyCompactCard(
                    data = state.daily,
                    challengeName = challengeName,
                    onClick = onDailyTap,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(HuezooSpacing.md))

            StaggeredCard(index = 3) {
                LeaderboardCompactCard(
                    rank = state.rank,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(HuezooSpacing.lg))

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
            // Cyan accent bar
            Box(
                modifier = Modifier
                    .width(CyanAccentBarWidth)
                    .fillMaxHeight()
                    .background(HuezooColors.AccentCyan),
            )
            // Gems content
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(HuezooColors.SurfaceL2)
                    .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.sm + 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    HuezooDisplayMedium(
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
    playerLevel: PlayerLevel,
    totalGems: Int,
    onEnterGame: () -> Unit,
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

    val progressFraction = levelProgressFraction(playerLevel, totalGems)
    val nextLevelName = nextLevelName(playerLevel)

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
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(
                        color = if (enabled) {
                            HuezooColors.AccentCyan.copy(alpha = pulseAlpha)
                        } else {
                            HuezooColors.TextDisabled
                        },
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

            HuezooTitleLarge(
                text = "THE\nTHRESHOLD",
                color = if (enabled) HuezooColors.TextPrimary else HuezooColors.TextSecondary,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(Modifier.height(HuezooSpacing.sm))

            HuezooBodyMedium(
                text = "Analyze chromatic anomalies. Detect the odd color — precision test for the elite observer.",
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

            // CTA button — only this navigates (card is not tappable at root level)
            HuezooButton(
                text = if (enabled) "ENTER SIMULATION" else "NO TRIES LEFT",
                onClick = onEnterGame,
                enabled = enabled,
                variant = if (enabled) HuezooButtonVariant.Primary else HuezooButtonVariant.GhostDanger,
            )

            Spacer(Modifier.height(HuezooSpacing.lg))

            LevelProgressBar(
                fraction = progressFraction,
                currentLevel = playerLevel,
                nextLevelName = nextLevelName,
                accentColor = playerLevel.levelColor,
            )
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
    val accentColor = HuezooColors.AccentCyan
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
        val arcStart = 140f   // degrees — leftward-facing sweep
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
            sw
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
                (cy + sweepLen * sin(sweepRad)).toFloat()
            ),
            strokeWidth = sw * 1.6f,
        )
        val ghostRad = (sweepDeg - 14.0) * PI / 180.0
        drawLine(
            color = baseColor.copy(0.10f * dimFactor),
            start = Offset(cx, cy),
            end = Offset(
                (cx + sweepLen * cos(ghostRad)).toFloat(),
                (cy + sweepLen * sin(ghostRad)).toFloat()
            ),
            strokeWidth = sw,
        )

        // ── 5. Center focal diamond + cyan dot ────────────────────────────────
        val d = 9.dp.toPx()
        val diamond = Path().apply {
            moveTo(cx, cy - d); lineTo(cx + d, cy); lineTo(cx, cy + d); lineTo(cx - d, cy); close()
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
                    Size(bSz, bSz)
                )
            }
        }

        // ── 7. Military corner brackets ───────────────────────────────────────
        val bLen = 12.dp.toPx()
        val bAlpha = 0.22f * dimFactor
        val bSw = sw * 1.8f

        data class Corner(val x: Float, val y: Float, val dx: Float, val dy: Float)
        listOf(
            Corner(0f, 0f, 1f, 1f),               // top-left
            Corner(w, 0f, -1f, 1f),               // top-right
            Corner(0f, h, 1f, -1f),               // bottom-left
            Corner(w, h, -1f, -1f),               // bottom-right
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
    nextLevelName: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
            if (nextLevelName != null) {
                HuezooLabelSmall(text = nextLevelName, color = HuezooColors.TextDisabled)
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
    val subtitleText = when {
        data.isCompletedToday -> countdown ?: "Completed today"
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
 * Global Leaderboard compact card — full width, stubbed until Firebase.
 */
@Composable
private fun LeaderboardCompactCard(
    rank: Int?,
    modifier: Modifier = Modifier,
) {
    CompactCard(
        label = "GLOBAL LEADERBOARD",
        title = if (rank != null) "RANK #$rank" else "TOP 5% WORLDWIDE",
        subtitle = "Claim weekly rewards",
        accentColor = HuezooColors.AccentYellow,
        enabled = false,
        onClick = {},
        iconDraw = { color -> drawLeaderboardBars(color) },
        modifier = modifier,
    )
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
            .border(1.5.dp, HuezooColors.AccentCyan.copy(alpha = 0.28f), cardShape)
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
                val w = size.width
                val h = size.height
                drawLine(
                    HuezooColors.AccentCyan.copy(alpha = 0.7f),
                    Offset(w * 0.15f, h * 0.38f),
                    Offset(w * 0.50f, h * 0.65f),
                    sw,
                    StrokeCap.Round,
                )
                drawLine(
                    HuezooColors.AccentCyan.copy(alpha = 0.7f),
                    Offset(w * 0.50f, h * 0.65f),
                    Offset(w * 0.85f, h * 0.38f),
                    sw,
                    StrokeCap.Round,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = HuezooSpacing.md,
                    end = HuezooSpacing.md,
                    bottom = HuezooSpacing.md,
                ),
            ) {
                HuezooBodyMedium(
                    text = "ΔE is how different two colors look to your eyes.\n\nThink of it like " +
                            "this — ΔE 10 is like red vs blue, obvious. ΔE 1 is like two blues " +
                            "that look almost the same. The lower the number, the sneakier " +
                            "the odd color is.",
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
    val next = PlayerLevel.entries.getOrNull(level.ordinal + 1) ?: return 1f
    val range = (next.minGems - level.minGems).toFloat()
    val progress = (totalGems - level.minGems).toFloat()
    return (progress / range).coerceIn(0f, 1f)
}

private fun nextLevelName(level: PlayerLevel): String? =
    PlayerLevel.entries.getOrNull(level.ordinal + 1)?.displayName

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
