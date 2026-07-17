package xyz.ksharma.huezoo.ui.games.colormemory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lexilabs.basic.ads.AdState
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.InterstitialAd
import app.lexilabs.basic.ads.composable.rememberInterstitialAd
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.debug.DebugFlags
import xyz.ksharma.huezoo.platform.ads.AdIds
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.ColorMemoryHelpSheet
import xyz.ksharma.huezoo.ui.components.HuezooAlertDialog
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooDisplaySmall
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTitleLarge
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.MemoryChamber
import xyz.ksharma.huezoo.ui.components.MemoryChamberState
import xyz.ksharma.huezoo.ui.components.RoundIndicator
import xyz.ksharma.huezoo.ui.components.SkewedStatChip
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMRoundResult
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMatchNavEvent
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMatchPhase
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMatchUiEvent
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMatchUiState
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.SquircleLarge
import xyz.ksharma.huezoo.ui.theme.rimLight
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val ChamberGap = 10.dp
private val PhaseHeadlineMinHeight = 56.dp
private const val WRONG_SHAKE_MS = 450
private const val WRONG_SHAKE_AMPLITUDE = 12f
private const val DELTA_E_TIER_EASY = 3.5f
private const val DELTA_E_TIER_MID = 2f
private const val DELTA_E_TIER_HARD = 1f
private const val DELTA_E_COLOR_EASY = 3f
private const val DECIMAL_SCALE = 10
private val FeedbackShelfOffset = 6.dp

@Composable
fun CMMatchScreen(
    onResult: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CMMatchViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPaid by viewModel.isPaid.collectAsStateWithLifecycle()
    val showInterstitial by viewModel.showInterstitial.collectAsStateWithLifecycle()
    var showHelp by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    val currentOnResult by rememberUpdatedState(onResult)

    // Pre-load the replay interstitial unconditionally (composable ordering); only shown when
    // a free-tier player taps Play Again. Mirrors the Threshold screen's pattern.
    @OptIn(DependsOnGoogleMobileAds::class)
    val interstitialAdState = rememberInterstitialAd(
        adUnitId = AdIds.interstitial,
        onLoad = {},
        onFailure = { _ -> },
    )

    if (showHelp) {
        ColorMemoryHelpSheet(onDismiss = { showHelp = false })
    }
    if (showLeaveDialog) {
        HuezooAlertDialog(
            title = "LEAVE SESSION?",
            message = "Progress isn't saved — a session only counts when all 10 rounds " +
                "are complete.",
            confirmText = "LEAVE",
            confirmVariant = HuezooButtonVariant.GhostDanger,
            onConfirm = onBack,
            dismissText = "KEEP PLAYING",
            onDismissRequest = { showLeaveDialog = false },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                CMMatchNavEvent.NavigateToResult -> currentOnResult()
            }
        }
    }

    // On (re)entry: first entry + config changes are no-ops; returning after a finished
    // session (Play Again) restarts — paid immediately, free after the interstitial.
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }

    Box(modifier = modifier) {
        AmbientGlowBackground(
            modifier = Modifier.fillMaxSize(),
            primaryColor = HuezooColors.AccentPurple,
            secondaryColor = HuezooColors.AccentCyan,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HuezooTopBar(
                    // Mid-session back → confirm; a session only counts when completed.
                    onBackClick = { showLeaveDialog = true },
                    currencyAmount = null,
                    onHelpClick = { showHelp = true },
                )

                when (val state = uiState) {
                    CMMatchUiState.Loading -> Unit
                    is CMMatchUiState.Playing -> CMMatchPlayingContent(
                        state = state,
                        onAnswer = { saidSame -> viewModel.onUiEvent(CMMatchUiEvent.Answer(saidSame)) },
                    )
                }
            }
        }

        // Interstitial before a free-tier replay. Skip gracefully if the ad isn't ready.
        @OptIn(DependsOnGoogleMobileAds::class)
        if (showInterstitial && !DebugFlags.hideAds) {
            when (interstitialAdState.value.state) {
                AdState.READY, AdState.SHOWING, AdState.SHOWN -> InterstitialAd(
                    loadedAd = interstitialAdState.value,
                    onDismissed = { viewModel.onInterstitialDone() },
                    onFailure = { _ -> viewModel.onInterstitialDone() },
                )
                else -> LaunchedEffect(showInterstitial) { viewModel.onInterstitialDone() }
            }
        } else if (showInterstitial) {
            // Debug "hide ads" on — skip straight to the replay.
            LaunchedEffect(showInterstitial) { viewModel.onInterstitialDone() }
        }
    }
}

// ── Playing content ───────────────────────────────────────────────────────────

@Composable
private fun CMMatchPlayingContent(
    state: CMMatchUiState.Playing,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.sm),
        ) {
            // ── HUD: round + score chips, round dots ──────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm)) {
                SkewedStatChip(
                    label = "ROUND",
                    value = "${state.round}/${state.totalRounds}",
                    accentColor = HuezooColors.AccentCyan,
                )
                SkewedStatChip(
                    label = "SCORE",
                    value = "${state.score}",
                    accentColor = HuezooColors.AccentYellow,
                )
            }
            Spacer(Modifier.height(HuezooSpacing.sm))
            RoundIndicator(
                totalRounds = state.totalRounds,
                currentRound = state.round,
                activeColor = HuezooColors.AccentPurple,
                results = state.roundResults.map { it == CMMRoundResult.Correct },
            )

            // ── Phase headline (short) — fixed min-height so nothing shifts ────
            PhaseHeadline(
                phase = state.phase,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HuezooSpacing.md)
                    .defaultMinSize(minHeight = PhaseHeadlineMinHeight),
            )

            // ── Twin chambers — flex to fill remaining space (no scroll) ──────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = HuezooSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(ChamberGap),
            ) {
                MemoryChamber(
                    label = "A",
                    state = when (state.phase) {
                        CMMatchPhase.Memory -> MemoryChamberState.Live
                        CMMatchPhase.Hold, CMMatchPhase.Recall -> MemoryChamberState.Sealed
                        CMMatchPhase.Feedback -> MemoryChamberState.Revealed
                    },
                    color = state.colorA,
                    accent = HuezooColors.AccentCyan,
                    entranceKey = state.roundGeneration,
                    showUnsealedPill = true,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
                MemoryChamber(
                    label = "B",
                    state = when (state.phase) {
                        CMMatchPhase.Memory, CMMatchPhase.Hold -> MemoryChamberState.Waiting
                        CMMatchPhase.Recall -> MemoryChamberState.Live
                        CMMatchPhase.Feedback -> MemoryChamberState.Revealed
                    },
                    color = state.colorB,
                    accent = HuezooColors.AccentMagenta,
                    entranceKey = state.roundGeneration,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
            }

            // ── ΔE readout (compact, single line) ─────────────────────────────
            Row(verticalAlignment = Alignment.Bottom) {
                HuezooLabelSmall(
                    text = "THIS ROUND",
                    color = HuezooColors.TextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
                Spacer(Modifier.width(HuezooSpacing.sm))
                HuezooDisplaySmall(
                    text = "ΔE ${state.roundDeltaE.fmt()}",
                    color = deltaEColor(state.roundDeltaE),
                )
                Spacer(Modifier.width(HuezooSpacing.sm))
                HuezooLabelSmall(
                    text = deltaETierLabel(state.roundDeltaE),
                    color = deltaEColor(state.roundDeltaE).copy(alpha = 0.8f),
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }

            // ── SAME / DIFFERENT — always visible, pinned above nav bar ────────
            AnswerButtons(
                state = state,
                onAnswer = onAnswer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HuezooSpacing.sm),
            )
        }

        // ── Feedback reveal card — cool overlay, auto-dismisses on advance ────
        val answer = state.lastAnswer
        AnimatedVisibility(
            visible = state.phase == CMMatchPhase.Feedback && answer != null,
            enter = fadeIn(tween(200)) + scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.8f),
            exit = fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 0.9f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            if (answer != null) {
                FeedbackRevealCard(
                    correct = answer.correct,
                    truthSame = answer.truthSame,
                    sting = answer.sting,
                    modifier = Modifier.padding(horizontal = HuezooSpacing.xl),
                )
            }
        }
    }
}

@Composable
private fun PhaseHeadline(
    phase: CMMatchPhase,
    modifier: Modifier = Modifier,
) {
    val (tag, tagColor, title) = when (phase) {
        CMMatchPhase.Memory -> Triple("MEMORISE", HuezooColors.AccentCyan, "Hold this colour.")
        CMMatchPhase.Hold -> Triple("SEALED", HuezooColors.AccentYellow, "Stand by…")
        CMMatchPhase.Recall -> Triple("RECALL", HuezooColors.AccentMagenta, "Same — or different?")
        CMMatchPhase.Feedback -> Triple("", Color.Transparent, "")
    }
    Column(modifier = modifier) {
        if (tag.isNotEmpty()) {
            HuezooLabelSmall(text = "◉ $tag", color = tagColor, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(HuezooSpacing.xs))
            HuezooTitleLarge(text = title, color = HuezooColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun AnswerButtons(
    state: CMMatchUiState.Playing,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = state.phase == CMMatchPhase.Recall
    val answer = state.lastAnswer
    val isWrong = answer != null && !answer.correct
    val shakeSame = isWrong && !answer.truthSame
    val shakeDifferent = isWrong && answer.truthSame

    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(answer) {
        if (isWrong && state.phase == CMMatchPhase.Feedback) {
            shakeX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = WRONG_SHAKE_MS
                    -WRONG_SHAKE_AMPLITUDE at WRONG_SHAKE_MS / 6
                    WRONG_SHAKE_AMPLITUDE at WRONG_SHAKE_MS / 3
                    -WRONG_SHAKE_AMPLITUDE * 0.6f at WRONG_SHAKE_MS / 2
                    WRONG_SHAKE_AMPLITUDE * 0.6f at WRONG_SHAKE_MS * 2 / 3
                    0f at WRONG_SHAKE_MS
                },
            )
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm + HuezooSpacing.xs),
    ) {
        HuezooButton(
            text = "SAME",
            onClick = { onAnswer(true) },
            variant = if (enabled) HuezooButtonVariant.Primary else HuezooButtonVariant.Ghost,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { if (shakeSame) translationX = shakeX.value },
        )
        HuezooButton(
            text = "DIFFERENT",
            onClick = { onAnswer(false) },
            variant = if (enabled) HuezooButtonVariant.Danger else HuezooButtonVariant.GhostDanger,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { if (shakeDifferent) translationX = shakeX.value },
        )
    }
}

/** The per-round reveal — a squircle card on a shelf, colour-coded to the outcome. */
@Composable
private fun FeedbackRevealCard(
    correct: Boolean,
    truthSame: Boolean,
    sting: String,
    modifier: Modifier = Modifier,
) {
    val accent = if (correct) HuezooColors.AccentGreen else HuezooColors.AccentMagenta
    Box(
        modifier = modifier
            .widthIn(max = 340.dp)
            .padding(bottom = FeedbackShelfOffset)
            .shapedShadow(SquircleLarge, HuezooColors.SurfaceL0, FeedbackShelfOffset, FeedbackShelfOffset)
            .background(HuezooColors.SurfaceL2, SquircleLarge)
            .clip(SquircleLarge)
            .rimLight(cornerRadius = 24.dp),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(accent))
            Column(
                modifier = Modifier.padding(HuezooSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HuezooLabelSmall(
                    text = if (correct) "◉ MEMORY VERIFIED" else "◉ MEMORY DRIFTED",
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(HuezooSpacing.xs))
                HuezooTitleLarge(
                    text = "TRUTH: ${if (truthSame) "SAME" else "DIFFERENT"}",
                    color = HuezooColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(HuezooSpacing.sm))
                HuezooLabelSmall(
                    text = sting,
                    color = HuezooColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                Spacer(Modifier.height(HuezooSpacing.sm))
                HuezooLabelSmall(
                    text = if (correct) "+10 PTS" else "−5 PTS",
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun deltaEColor(deltaE: Float): Color = when {
    deltaE > DELTA_E_COLOR_EASY -> HuezooColors.AccentCyan
    deltaE > DELTA_E_TIER_HARD -> HuezooColors.AccentYellow
    else -> HuezooColors.AccentMagenta
}

private fun deltaETierLabel(deltaE: Float): String = when {
    deltaE >= DELTA_E_TIER_EASY -> "EASY"
    deltaE >= DELTA_E_TIER_MID -> "MEDIUM"
    deltaE >= DELTA_E_TIER_HARD -> "HARD"
    else -> "ELITE"
}

private fun Float.fmt(): String {
    val i = toInt()
    val d = ((this - i) * DECIMAL_SCALE).toInt()
    return "$i.$d"
}

// ── Previews ─────────────────────────────────────────────────────────────────

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@Composable
private fun CMMatchMemoryPhasePreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        CMMatchPlayingContent(
            state = CMMatchUiState.Playing(
                round = 3,
                totalRounds = 10,
                phase = CMMatchPhase.Memory,
                score = 15,
                roundResults = listOf(CMMRoundResult.Correct, CMMRoundResult.Wrong),
                colorA = HuezooColors.AccentPurple,
                colorB = HuezooColors.AccentPurple,
                currentDeltaE = 3.0f,
                roundDeltaE = 3.0f,
            ),
            onAnswer = {},
        )
    }
}

@xyz.ksharma.huezoo.ui.preview.PreviewScreen
@Composable
private fun CMMatchRecallPhasePreview() {
    xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme {
        CMMatchPlayingContent(
            state = CMMatchUiState.Playing(
                round = 8,
                totalRounds = 10,
                phase = CMMatchPhase.Recall,
                score = 55,
                roundResults = List(7) { CMMRoundResult.Correct },
                colorA = HuezooColors.AccentCyan,
                colorB = HuezooColors.AccentCyan,
                currentDeltaE = 1.0f,
                roundDeltaE = 1.0f,
            ),
            onAnswer = {},
        )
    }
}
