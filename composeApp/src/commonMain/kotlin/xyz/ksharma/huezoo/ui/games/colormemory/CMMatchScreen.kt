package xyz.ksharma.huezoo.ui.games.colormemory

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.BannerAd
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.debug.DebugFlags
import xyz.ksharma.huezoo.platform.ads.AdIds
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.ColorMemoryHelpSheet
import xyz.ksharma.huezoo.ui.components.DeltaEBadge
import xyz.ksharma.huezoo.ui.components.HuezooBodyMedium
import xyz.ksharma.huezoo.ui.components.HuezooBottomSheet
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
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

private val ChamberMinHeight = 320.dp
private val ChamberGap = 10.dp
private val IdentityDividerHeight = 3.dp
private val PhaseHeadlineMinHeight = 64.dp
private const val WRONG_SHAKE_MS = 450
private const val WRONG_SHAKE_AMPLITUDE = 12f
private const val DELTA_E_TIER_EASY = 3.5f
private const val DELTA_E_TIER_MID = 2f
private const val DELTA_E_TIER_HARD = 1f
private const val DELTA_E_COLOR_EASY = 3f
private const val DECIMAL_SCALE = 10

@Composable
fun CMMatchScreen(
    onResult: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CMMatchViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPaid by viewModel.isPaid.collectAsStateWithLifecycle()
    var showHelp by remember { mutableStateOf(false) }
    var showLeaveSheet by remember { mutableStateOf(false) }
    val currentOnResult by rememberUpdatedState(onResult)

    if (showHelp) {
        ColorMemoryHelpSheet(onDismiss = { showHelp = false })
    }
    if (showLeaveSheet) {
        LeaveSessionSheet(
            onLeave = {
                showLeaveSheet = false
                onBack()
            },
            onStay = { showLeaveSheet = false },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                CMMatchNavEvent.NavigateToResult -> currentOnResult()
            }
        }
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
                    onBackClick = { showLeaveSheet = true },
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

        @OptIn(DependsOnGoogleMobileAds::class)
        if (!isPaid && !DebugFlags.hideAds && uiState !is CMMatchUiState.Loading) {
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
    }
}

// ── Playing content ───────────────────────────────────────────────────────────

@Composable
private fun CMMatchPlayingContent(
    state: CMMatchUiState.Playing,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        // ── Sub-header: game tag + round ΔE badge ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HuezooSpacing.sm + HuezooSpacing.xs)
                .padding(horizontal = HuezooSpacing.md + HuezooSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HuezooLabelSmall(
                text = "◉ GAME 6 · MEMORY MATCH",
                color = HuezooColors.AccentPurple,
                fontWeight = FontWeight.ExtraBold,
            )
            DeltaEBadge(deltaE = state.roundDeltaE, label = "ROUND ΔE")
        }

        // ── Identity divider ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HuezooSpacing.sm)
                .padding(horizontal = HuezooSpacing.md + HuezooSpacing.xs)
                .height(IdentityDividerHeight)
                .background(HuezooColors.AccentPurple),
        )

        // ── HUD: stat chips + round dots ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HuezooSpacing.sm)
                .padding(horizontal = HuezooSpacing.md + HuezooSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
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
            RoundIndicator(
                totalRounds = state.totalRounds,
                currentRound = state.round,
                activeColor = HuezooColors.AccentPurple,
                results = state.roundResults.map { it == CMMRoundResult.Correct },
            )
        }

        // ── Phase headline — fixed min height so nothing below ever shifts ────
        PhaseHeadline(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HuezooSpacing.md)
                .padding(horizontal = HuezooSpacing.lg)
                .defaultMinSize(minHeight = PhaseHeadlineMinHeight),
        )

        // ── Twin chambers ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HuezooSpacing.sm)
                .padding(horizontal = HuezooSpacing.md + HuezooSpacing.xs)
                .height(ChamberMinHeight),
            horizontalArrangement = Arrangement.spacedBy(ChamberGap),
        ) {
            MemoryChamber(
                label = "CHAMBER A",
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
                label = "CHAMBER B",
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

        // ── ΔE meta row ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HuezooSpacing.md)
                .padding(horizontal = HuezooSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    HuezooLabelSmall(
                        text = "THIS ROUND",
                        color = HuezooColors.TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        xyz.ksharma.huezoo.ui.components.HuezooDisplaySmall(
                            text = "ΔE ${state.roundDeltaE.fmt()}",
                            color = deltaEColor(state.roundDeltaE),
                        )
                        Spacer(Modifier.padding(start = HuezooSpacing.sm))
                        HuezooLabelSmall(
                            text = deltaETierLabel(state.roundDeltaE),
                            color = deltaEColor(state.roundDeltaE).copy(alpha = 0.8f),
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = HuezooSpacing.xs),
                        )
                    }
                }
            }

            // Points feedback — only during Feedback phase
            val answer = state.lastAnswer
            val pointsAlpha by animateFloatAsState(
                targetValue = if (state.phase == CMMatchPhase.Feedback && answer != null) 1f else 0f,
                animationSpec = tween(160),
                label = "pointsAlpha",
            )
            HuezooLabelSmall(
                text = if (answer?.correct == true) "+10 PTS" else "−5 PTS",
                color = if (answer?.correct == true) HuezooColors.AccentGreen else HuezooColors.AccentMagenta,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.graphicsLayer { alpha = pointsAlpha },
            )
        }

        Spacer(Modifier.weight(1f))

        // ── SAME / DIFFERENT buttons ──────────────────────────────────────────
        AnswerButtons(
            state = state,
            onAnswer = onAnswer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HuezooSpacing.sm + HuezooSpacing.xs)
                .padding(horizontal = HuezooSpacing.md + HuezooSpacing.xs)
                .padding(bottom = HuezooSpacing.lg),
        )
    }
}

@Composable
private fun PhaseHeadline(
    state: CMMatchUiState.Playing,
    modifier: Modifier = Modifier,
) {
    val answer = state.lastAnswer
    val (tag, tagColor, title) = when (state.phase) {
        CMMatchPhase.Memory -> Triple("◉ CHAMBER A LIVE", HuezooColors.AccentCyan, "Hold this color.")
        CMMatchPhase.Hold -> Triple("◉ CHAMBER A SEALED", HuezooColors.AccentYellow, "Stand by…")
        CMMatchPhase.Recall -> Triple(
            "◉ CHAMBER B LIVE — RECALL",
            HuezooColors.AccentMagenta,
            "Match the seal?",
        )
        CMMatchPhase.Feedback -> if (answer?.correct == true) {
            Triple("◉ MEMORY VERIFIED", HuezooColors.AccentGreen, answer.sting)
        } else {
            Triple("◉ MEMORY DRIFTED", HuezooColors.AccentMagenta, answer?.sting ?: "")
        }
    }

    Column(modifier = modifier) {
        HuezooLabelSmall(
            text = tag,
            color = tagColor,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(HuezooSpacing.xs))
        HuezooTitleLarge(
            text = title,
            color = HuezooColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun AnswerButtons(
    state: CMMatchUiState.Playing,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = state.phase == CMMatchPhase.Recall

    // Wrong-answer shake on the chosen-but-wrong side
    val answer = state.lastAnswer
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(answer) {
        if (answer != null && !answer.correct && state.phase == CMMatchPhase.Feedback) {
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
    // A wrong answer means the player tapped the OPPOSITE of the truth: they said
    // SAME when it was DIFFERENT, or DIFFERENT when it was SAME. Shake the tapped side.
    val isWrong = answer != null && !answer.correct
    val shakeSame = isWrong && !answer.truthSame
    val shakeDifferent = isWrong && answer.truthSame

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

// ── Leave-session confirmation ────────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LeaveSessionSheet(
    onLeave: () -> Unit,
    onStay: () -> Unit,
) {
    HuezooBottomSheet(onDismissRequest = onStay) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HuezooSpacing.lg)
                .navigationBarsPadding()
                .padding(bottom = HuezooSpacing.xl),
        ) {
            HuezooTitleLarge(
                text = "LEAVE SESSION?",
                color = HuezooColors.AccentPurple,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(HuezooSpacing.sm))
            HuezooBodyMedium(
                text = "Progress isn't saved — a session only counts when all " +
                    "10 rounds are complete.",
                color = HuezooColors.TextSecondary,
            )
            Spacer(Modifier.height(HuezooSpacing.lg))
            HuezooButton(
                text = "KEEP PLAYING",
                onClick = onStay,
                variant = HuezooButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(HuezooSpacing.sm))
            HuezooButton(
                text = "LEAVE",
                onClick = onLeave,
                variant = HuezooButtonVariant.GhostDanger,
                modifier = Modifier.fillMaxWidth(),
            )
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
