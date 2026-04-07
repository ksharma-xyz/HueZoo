@file:Suppress("TooManyFunctions")

package xyz.ksharma.huezoo.ui.games.threshold

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lexilabs.basic.ads.AdState
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.BannerAd
import app.lexilabs.basic.ads.composable.InterstitialAd
import app.lexilabs.basic.ads.composable.rememberInterstitialAd
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.debug.DebugFlags
import xyz.ksharma.huezoo.platform.ads.AdIds
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.DeltaEBadge
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTitleMedium
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.RadialSwatchLayout
import xyz.ksharma.huezoo.ui.components.ThresholdHelpSheet
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdNavEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiState
import xyz.ksharma.huezoo.ui.model.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.theme.HeartLife
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("CyclomaticComplexMethod")
@Composable
fun ThresholdScreen(
    onResult: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThresholdViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPaid by viewModel.isPaid.collectAsStateWithLifecycle()
    val showInterstitial by viewModel.showInterstitial.collectAsStateWithLifecycle()
    var showHelp by remember { mutableStateOf(false) }

    // Pre-load the interstitial at screen entry so it's ready when the session ends.
    // Must be called unconditionally (composable ordering rules) — only used when !isPaid.
    // This avoids the crash where InterstitialAd.setListeners() throws if called before load completes.
    @OptIn(DependsOnGoogleMobileAds::class)
    val interstitialAdState = rememberInterstitialAd(
        adUnitId = AdIds.interstitial,
        onLoad = {},
        onFailure = { _ -> /* handled at show time via the else branch below */ },
    )

    if (showHelp) {
        ThresholdHelpSheet(onDismiss = { showHelp = false })
    }

    // Collect nav events. A fresh subscriber on config-change will start collecting
    // future events — the SharedFlow buffer handles any in-flight event safely.
    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                ThresholdNavEvent.NavigateToResult -> onResult()
                ThresholdNavEvent.NavigateBack -> onBack()
            }
        }
    }

    // Re-check attempt status only when blocked (e.g. cooldown expired while backgrounded).
    // LifecycleResumeEffect fires on every RESUME — safe because onResume() never resets
    // active gameplay; viewModelScope coroutines survive config changes on their own.
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }

    Box(modifier = modifier) {
        AmbientGlowBackground(
            modifier = Modifier.fillMaxSize(),
            primaryColor = HuezooColors.GameThreshold,
            secondaryColor = HuezooColors.AccentPurple,
        ) {
            // Cine-style floor/ceiling reflection lines — drawn behind all game UI.
            // Canvas is a Spacer internally so it never consumes touch events.
            ThresholdLightLines(modifier = Modifier.fillMaxSize())

            Column(modifier = Modifier.fillMaxSize()) {
                HuezooTopBar(
                    onBackClick = onBack,
                    currencyAmount = (uiState as? ThresholdUiState.Playing)?.totalGems,
                    onHelpClick = { showHelp = true },
                )

                when (val state = uiState) {
                    ThresholdUiState.Loading -> Unit
                    is ThresholdUiState.Blocked -> BlockedContent(
                        state = state,
                        onBack = onBack,
                    )
                    is ThresholdUiState.Playing -> PlayingContent(
                        state = state,
                        onSwatchTap = { index ->
                            viewModel.onUiEvent(ThresholdUiEvent.SwatchTapped(index))
                        },
                    )
                }
            }
        }

        @OptIn(DependsOnGoogleMobileAds::class)
        if (!isPaid && !DebugFlags.hideAds && uiState !is ThresholdUiState.Loading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                BannerAd(adUnitId = AdIds.banner)
            }
        }

        // Only show when the pre-loaded ad is READY — calling InterstitialAd before the
        // async load completes crashes with "InterstitialAd not loaded yet".
        @OptIn(DependsOnGoogleMobileAds::class)
        if (showInterstitial && !DebugFlags.hideAds) {
            when (interstitialAdState.value.state) {
                AdState.READY -> InterstitialAd(
                    loadedAd = interstitialAdState.value,
                    onDismissed = { viewModel.onInterstitialDone() },
                    onFailure = { _ -> viewModel.onInterstitialDone() },
                )
                AdState.SHOWING, AdState.SHOWN -> {
                    // Ad is on screen — onDismissed will call onInterstitialDone()
                }
                else -> {
                    // Ad not ready (still loading or failed) — skip ad and go to result
                    LaunchedEffect(showInterstitial) { viewModel.onInterstitialDone() }
                }
            }
        }

        // Perception Wall celebration — full-screen overlay, sits on top of everything.
        // ViewModel holds this phase for ANIMATION_PERCEPTION_WALL_MS then transitions automatically.
        if ((uiState as? ThresholdUiState.Playing)?.roundPhase == RoundPhase.PerceptionWall) {
            PerceptionWallOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun PlayingContent(
    state: ThresholdUiState.Playing,
    onSwatchTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // ── UX.12: Streak milestone burst trigger ─────────────────────────────────
    // streakMilestone is 5 or 10 in the Correct phase copy only; 0 every other time.
    var showStreakBurst by remember { mutableStateOf(false) }
    var burstMilestone by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.streakMilestone) {
        if (state.streakMilestone > 0) {
            burstMilestone = state.streakMilestone
            showStreakBurst = true
            delay(1800L)
            showStreakBurst = false
        }
    }

    // ── UX.9.3: Gem float-up ─────────────────────────────────────────────────
    var gemFloatDelta by remember { mutableIntStateOf(0) }
    var gemFloatVisible by remember { mutableStateOf(false) }
    val gemFloatOffsetY = remember { Animatable(0f) }
    val gemFloatAlpha = remember { Animatable(0f) }
    var prevGems by remember { mutableStateOf(state.totalGems) }
    LaunchedEffect(state.totalGems) {
        val delta = state.totalGems - prevGems
        prevGems = state.totalGems
        if (delta > 0) {
            gemFloatDelta = delta
            gemFloatOffsetY.snapTo(0f)
            gemFloatAlpha.snapTo(1f)
            gemFloatVisible = true
            gemFloatOffsetY.animateTo(-72f, tween(600, easing = FastOutSlowInEasing))
            gemFloatAlpha.animateTo(0f, tween(200))
            gemFloatVisible = false
        }
    }

    val accent = LocalPlayerAccentColor.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(HuezooSpacing.sm))

        // ── Header row: title left, lives hearts right ────────────────────────
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
        ) {
            HuezooTitleMedium(
                text = "IDENTIFY  THE  OUTLIER",
                color = accent,
                fontWeight = FontWeight.ExtraBold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.xs)) {
                repeat(state.maxAttempts) { index ->
                    val isRemaining = index < state.attemptsRemaining
                    val fillColor by animateColorAsState(
                        targetValue = if (isRemaining) accent else accent.copy(alpha = 0f),
                        animationSpec = tween(300),
                        label = "lifeHeart_$index",
                    )
                    Box(
                        modifier = Modifier
                            .size(HEART_SIZE)
                            .border(
                                width = 1.5.dp,
                                color = accent.copy(alpha = 0.4f),
                                shape = HeartLife,
                            )
                            .background(fillColor, HeartLife),
                    )
                }
            }
        }

        // ── ΔE hero ───────────────────────────────────────────────────────────
        Spacer(Modifier.height(HuezooSpacing.xl))

        // UX.9.3: Gem float-up anchored above the DeltaEBadge
        Box(contentAlignment = Alignment.TopCenter) {
            DeltaEBadge(deltaE = state.deltaE)
            if (gemFloatVisible) {
                Text(
                    text = "+$gemFloatDelta 💎",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = HuezooColors.GemGreen,
                    modifier = Modifier.graphicsLayer {
                        translationY = gemFloatOffsetY.value
                        alpha = gemFloatAlpha.value
                    },
                )
            }
        }

        // UX.7.4: First-round tooltip — fades out after first correct tap
        val tooltipAlpha by animateFloatAsState(
            targetValue = if (state.tap == 1 && state.roundPhase == RoundPhase.Idle) 1f else 0f,
            animationSpec = tween(500),
            label = "tooltipAlpha",
        )
        HuezooLabelSmall(
            text = "Lower ΔE = harder to spot",
            color = HuezooColors.TextSecondary.copy(alpha = 0.55f),
            modifier = Modifier.graphicsLayer { alpha = tooltipAlpha },
        )

        Spacer(Modifier.height(HuezooSpacing.sm))

        // SESSION BEST — fades in after first correct tap, stays for the session
        val bestAlpha by animateFloatAsState(
            targetValue = if (state.sessionBestDeltaE != null) 1f else 0f,
            animationSpec = tween(durationMillis = 400),
            label = "bestDeltaEAlpha",
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.graphicsLayer { alpha = bestAlpha },
        ) {
            HuezooLabelSmall(text = "SESSION BEST", color = HuezooColors.TextSecondary)
            HuezooLabelSmall(
                text = state.sessionBestDeltaE?.fmt() ?: "--",
                color = HuezooColors.AccentGreen,
            )
        }

        Spacer(Modifier.height(HuezooSpacing.md))

        // ── Fixed-height feedback slot ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FEEDBACK_SLOT_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            val correctAlpha by animateFloatAsState(
                targetValue = if (state.roundPhase == RoundPhase.Correct) 1f else 0f,
                animationSpec = tween(durationMillis = 160),
                label = "feedbackCorrectAlpha",
            )
            Text(
                text = "↓ ΔE ${state.deltaE.fmt()} — SHARPER",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = HuezooColors.AccentGreen,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = correctAlpha },
            )

            val wrongAlpha by animateFloatAsState(
                targetValue = if (state.roundPhase == RoundPhase.Wrong) 1f else 0f,
                animationSpec = tween(durationMillis = 160),
                label = "feedbackWrongAlpha",
            )
            Text(
                text = state.stingCopy.orEmpty(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = HuezooColors.AccentMagenta,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = wrongAlpha },
            )
        }

        Spacer(Modifier.height(HuezooSpacing.md))

        // ── UX.12: Swatch layout + streak particle burst ──────────────────────
        // StreakBurstOverlay is a transparent Canvas — doesn't consume touches.
        Box(contentAlignment = Alignment.Center) {
            RadialSwatchLayout(
                swatches = state.swatches,
                roundPhase = state.roundPhase,
                roundKey = state.roundGeneration,
                layoutStyle = state.layoutStyle,
                onSwatchTap = onSwatchTap,
            )
            // Floating particles rise up and fade — no background, no touch blocking
            StreakBurstOverlay(
                milestone = burstMilestone,
                active = showStreakBurst,
                accentColor = accent,
                modifier = Modifier.matchParentSize(),
            )
        }

        Spacer(Modifier.height(HuezooSpacing.md))
    }
}

// UX.3.2 — Styled blocked screen: live countdown + back button
@OptIn(ExperimentalTime::class)
@Composable
private fun BlockedContent(
    state: ThresholdUiState.Blocked,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val countdown by countdownUntil(state.nextResetAt)

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(HuezooSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = "OUT OF TRIES",
            style = MaterialTheme.typography.headlineMedium,
            color = HuezooColors.AccentMagenta,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(HuezooSpacing.sm))
        Text(
            text = "${state.attemptsUsed}/${state.maxAttempts} used this window",
            style = MaterialTheme.typography.bodyMedium,
            color = HuezooColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(HuezooSpacing.xl))
        if (countdown.isNotEmpty()) {
            Text(
                text = "Resets in $countdown",
                style = MaterialTheme.typography.titleMedium,
                color = HuezooColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.weight(1f))

        HuezooButton(
            text = "BACK TO HOME",
            onClick = onBack,
            variant = HuezooButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(HuezooSpacing.md))
    }
}

// ── Perception Wall celebration overlay ──────────────────────────────────────

/**
 * Full-screen legendary overlay shown when the player correctly identifies at ΔE 0.1.
 * Animates in with fade + scale; confetti falls throughout; eagle pulses with pride.
 * The ViewModel times the exit after [ANIMATION_PERCEPTION_WALL_MS].
 */
@Suppress("LongMethod")
@Composable
private fun PerceptionWallOverlay(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    val confettiProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        visible = true
        confettiProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = WALL_CONFETTI_DURATION_MS, easing = LinearEasing),
        )
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "wallOverlayAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.72f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "wallOverlayScale",
    )

    // Eagle heartbeat — pulses between 1.0 and 1.18 continuously while visible
    val eagleTransition = rememberInfiniteTransition(label = "eagle")
    val eagleScale by eagleTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eagleScale",
    )

    val confettiPieces = remember { generateConfetti() }
    val confettiT = confettiProgress.value

    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .background(HuezooColors.Background.copy(alpha = 0.93f)),
        contentAlignment = Alignment.Center,
    ) {
        // Confetti layer — sits behind all text content
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (confettiT == 0f) return@Canvas
            val confettiColors = listOf(
                HuezooColors.AccentCyan,
                HuezooColors.AccentMagenta,
                HuezooColors.AccentYellow,
                HuezooColors.AccentGreen,
            )
            confettiPieces.forEach { piece ->
                val rawT = ((confettiT - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
                if (rawT <= 0f) return@forEach
                val fallY = rawT * rawT * piece.speed * size.height
                val wobbleX = sin(rawT * piece.wobbleCycles * 2.0 * PI).toFloat() *
                    piece.wobbleAmp * size.width
                val pieceAlpha = (1f - ((rawT - 0.7f) / 0.3f).coerceIn(0f, 1f))
                if (pieceAlpha <= 0f) return@forEach
                drawCircle(
                    color = confettiColors[piece.colorIndex],
                    radius = piece.size * density,
                    center = Offset(
                        piece.x * size.width + wobbleX,
                        piece.startY * size.height + fallY,
                    ),
                    alpha = pieceAlpha,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .padding(horizontal = HuezooSpacing.xl),
        ) {
            Text(
                text = "🦅",
                fontSize = 72.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = eagleScale
                    scaleY = eagleScale
                },
            )

            Spacer(Modifier.height(HuezooSpacing.sm))

            Text(
                text = "YOU SHATTERED THE LIMIT",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = HuezooColors.AccentYellow,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp,
            )

            Text(
                text = "ΔE 0.1 · Most humans can't see this far",
                style = MaterialTheme.typography.bodyLarge,
                color = HuezooColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(HuezooSpacing.md))

            Text(
                text = "+5,000 💎",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = HuezooColors.GemGreen,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class ConfettiPiece(
    val x: Float, // normalized start X (0..1)
    val startY: Float, // normalized start Y — negative = above screen
    val speed: Float, // fall speed as fraction of screen height (per t unit)
    val size: Float, // radius in dp
    val colorIndex: Int, // index into confettiColors list
    val wobbleCycles: Float,
    val wobbleAmp: Float, // horizontal wobble amplitude as fraction of screen width
    val delay: Float, // 0..0.35 phase stagger
)

@Suppress("MagicNumber")
private fun generateConfetti(): List<ConfettiPiece> {
    val rng = Random(seed = 1337)
    return List(32) {
        ConfettiPiece(
            x = rng.nextFloat(),
            startY = -0.05f - rng.nextFloat() * 0.15f,
            speed = 0.55f + rng.nextFloat() * 0.6f,
            size = 4f + rng.nextFloat() * 7f,
            colorIndex = rng.nextInt(4),
            wobbleCycles = 1f + rng.nextFloat() * 2f,
            wobbleAmp = 0.015f + rng.nextFloat() * 0.035f,
            delay = rng.nextFloat() * 0.35f,
        )
    }
}

private const val WALL_CONFETTI_DURATION_MS = 2_500

/** Returns a live-updating "Xh Xm" string, ticking every 60 s. */
@OptIn(ExperimentalTime::class)
@Composable
private fun countdownUntil(until: Instant) = produceState(initialValue = "") {
    while (true) {
        val remaining = until - Clock.System.now()
        val totalSeconds = remaining.inWholeSeconds.coerceAtLeast(0)
        value = if (totalSeconds <= 0) {
            ""
        } else {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
        delay(60_000L)
    }
}

/** Fixed height reserved for the in-game feedback message. Never changes, so nothing shifts. */
private val FEEDBACK_SLOT_HEIGHT = 28.dp

/** Size of each heart in the lives indicator. */
private val HEART_SIZE = 14.dp

// ── Cine-style background light lines ────────────────────────────────────────

/**
 * Subtle horizontal light-reflection lines drawn full-screen behind the game UI.
 *
 * Mimics a cinema floor/ceiling reflection: lines cluster near the top and bottom edges,
 * fading toward the centre.  Each line pulses with the game's theme colour at very low
 * alpha (2–8%) so they add depth without distracting from gameplay.
 *
 * Implemented as [Canvas] (a Spacer under the hood) so it never intercepts touch events.
 */
@Composable
private fun ThresholdLightLines(modifier: Modifier = Modifier) {
    val lineColor = HuezooColors.GameThreshold
    Canvas(modifier = modifier) {
        val lineW = 1.2.dp.toPx()
        val lineCount = 9

        // Floor reflection: bottom 36% of screen — lines denser + brighter near very bottom
        for (i in 0 until lineCount) {
            val t = (i + 1).toFloat() / lineCount // 0 = near centre horizon, 1 = bottom edge
            val y = size.height - size.height * 0.36f * (1f - t * t) // quadratic spacing
            drawLine(
                color = lineColor.copy(alpha = 0.022f + t * 0.052f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = lineW,
            )
        }

        // Ceiling reflection: top 26% of screen — same pattern, mirrored
        for (i in 0 until lineCount - 2) {
            val t = (i + 1).toFloat() / (lineCount - 2)
            val y = size.height * 0.26f * (1f - t * t)
            drawLine(
                color = lineColor.copy(alpha = 0.018f + t * 0.038f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = lineW,
            )
        }
    }
}

// ── Streak burst overlay ──────────────────────────────────────────────────────

/**
 * Transparent Canvas-based particle burst that celebrates a streak milestone.
 *
 * Renders rising, wobbling circles in the player's accent color.
 * Does not draw any background and is layered as a Spacer — touches fall through.
 * Duration matches the 1 800 ms window kept open by [PlayingContent].
 */
@Composable
@Suppress("LongMethod")
private fun StreakBurstOverlay(
    milestone: Int,
    active: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active, milestone) {
        if (active) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = BURST_DURATION_MS.toInt(), easing = LinearEasing),
            )
        } else {
            progress.snapTo(0f)
        }
    }

    val particles = remember(milestone) { generateBurstParticles(milestone) }
    val t = progress.value // read in composable scope — drives recomposition each frame

    Canvas(modifier = modifier) {
        if (t == 0f) return@Canvas
        particles.forEach { p ->
            val rawLifetime = (t - p.phaseOffset) / (1f - p.phaseOffset).coerceAtLeast(0.01f)
            if (rawLifetime <= 0f || rawLifetime >= 1f) return@forEach

            val alpha = p.maxAlpha * (1f - rawLifetime * rawLifetime) // quadratic fade-out
            val riseY = rawLifetime * p.riseDistanceDp.dp.toPx()
            val wobbleX = sin(rawLifetime * p.wobbleCycles * 2.0 * PI).toFloat() *
                p.wobbleAmplitudeDp.dp.toPx()

            drawCircle(
                color = accentColor,
                radius = p.radiusDp.dp.toPx(),
                center = Offset(p.startXFrac * size.width + wobbleX, p.startYFrac * size.height - riseY),
                alpha = alpha,
            )
        }
    }
}

private data class BurstParticle(
    val startXFrac: Float,
    val startYFrac: Float,
    val radiusDp: Float,
    val riseDistanceDp: Float,
    val wobbleAmplitudeDp: Float,
    val wobbleCycles: Float,
    val phaseOffset: Float, // 0..0.5 — staggered launch within animation window
    val maxAlpha: Float,
)

@Suppress("MagicNumber")
private fun generateBurstParticles(milestone: Int): List<BurstParticle> {
    val rng = Random(seed = milestone)
    val count = if (milestone >= 10) 20 else 12
    val maxRadius = if (milestone >= 10) 8f else 5f
    return List(count) {
        BurstParticle(
            startXFrac = 0.1f + rng.nextFloat() * 0.8f,
            startYFrac = 0.3f + rng.nextFloat() * 0.5f,
            radiusDp = 3f + rng.nextFloat() * maxRadius,
            riseDistanceDp = 40f + rng.nextFloat() * 80f,
            wobbleAmplitudeDp = 4f + rng.nextFloat() * 12f,
            wobbleCycles = 0.8f + rng.nextFloat() * 1.5f,
            phaseOffset = rng.nextFloat() * 0.5f,
            maxAlpha = 0.45f + rng.nextFloat() * 0.45f,
        )
    }
}

private const val BURST_DURATION_MS = 1_600L

private fun Float.fmt(): String {
    val i = toInt()
    val d = ((this - i) * 10).toInt()
    return "$i.$d"
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val PREVIEW_PURPLE = androidx.compose.ui.graphics.Color(0xFF4CAF50)
private val PREVIEW_ODD = androidx.compose.ui.graphics.Color(0xFF66BB6A)

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun ThresholdPlayingPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        PlayingContent(
            state = ThresholdUiState.Playing(
                swatches = List(5) { xyz.ksharma.huezoo.ui.model.SwatchUiModel(PREVIEW_PURPLE) } +
                    listOf(xyz.ksharma.huezoo.ui.model.SwatchUiModel(PREVIEW_ODD)),
                deltaE = 2.4f,
                tap = 3,
                attemptsRemaining = 4,
                roundPhase = RoundPhase.Idle,
                totalGems = 24,
            ),
            onSwatchTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun ThresholdCorrectPhasePreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        PlayingContent(
            state = ThresholdUiState.Playing(
                swatches = listOf(
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(
                        PREVIEW_PURPLE,
                        displayState = SwatchDisplayState.Correct,
                    ),
                ) + List(5) { xyz.ksharma.huezoo.ui.model.SwatchUiModel(PREVIEW_PURPLE) },
                deltaE = 2.4f,
                tap = 3,
                attemptsRemaining = 4,
                roundPhase = RoundPhase.Correct,
                totalGems = 26,
            ),
            onSwatchTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
private fun ThresholdWrongPhasePreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        PlayingContent(
            state = ThresholdUiState.Playing(
                swatches = listOf(
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(
                        androidx.compose.ui.graphics.Color(0xFFE91E63),
                        displayState = SwatchDisplayState.Wrong,
                    ),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFFE91E63)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(
                        androidx.compose.ui.graphics.Color(0xFFF06292),
                        displayState = SwatchDisplayState.Revealed,
                    ),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFFE91E63)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFFE91E63)),
                    xyz.ksharma.huezoo.ui.model.SwatchUiModel(androidx.compose.ui.graphics.Color(0xFFE91E63)),
                ),
                deltaE = 1.8f,
                tap = 5,
                attemptsRemaining = 2,
                roundPhase = RoundPhase.Wrong,
                stingCopy = "So close. And yet.",
                totalGems = 32,
            ),
            onSwatchTap = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@androidx.compose.runtime.Composable
@kotlin.OptIn(kotlin.time.ExperimentalTime::class)
private fun ThresholdBlockedPreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        BlockedContent(
            state = ThresholdUiState.Blocked(
                nextResetAt = kotlin.time.Clock.System.now()
                    .plus(kotlin.time.Duration.parse("2h30m")),
                attemptsUsed = 5,
                maxAttempts = 5,
            ),
            onBack = {},
        )
    }
}
