package xyz.ksharma.huezoo.ui.games.threshold

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.data.repository.ThresholdRepository
import xyz.ksharma.huezoo.domain.color.ColorEngine
import xyz.ksharma.huezoo.domain.game.GameRewardRates
import xyz.ksharma.huezoo.domain.game.SessionResultCache
import xyz.ksharma.huezoo.domain.game.ThresholdGameEngine
import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.GemAward
import xyz.ksharma.huezoo.navigation.SessionResult
import xyz.ksharma.huezoo.platform.ads.AdOrchestrator
import xyz.ksharma.huezoo.platform.haptics.HapticEngine
import xyz.ksharma.huezoo.platform.haptics.HapticType
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdNavEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiState
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.model.PlayerState
import xyz.ksharma.huezoo.ui.model.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.model.SwatchLayoutStyle
import xyz.ksharma.huezoo.ui.model.SwatchUiModel
import xyz.ksharma.huezoo.ui.util.safeLaunch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ThresholdViewModel(
    private val gameEngine: ThresholdGameEngine,
    private val repository: ThresholdRepository,
    private val colorEngine: ColorEngine,
    private val settingsRepository: SettingsRepository,
    private val playerState: PlayerState,
    private val hapticEngine: HapticEngine,
    private val sessionResultCache: SessionResultCache,
    private val adOrchestrator: AdOrchestrator,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ThresholdUiState>(ThresholdUiState.Loading)
    val uiState: StateFlow<ThresholdUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ThresholdNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<ThresholdNavEvent> = _navEvent.asSharedFlow()

    private val _isPaid = MutableStateFlow(false)
    val isPaid: StateFlow<Boolean> = _isPaid.asStateFlow()

    private val _showInterstitial = MutableStateFlow(false)
    val showInterstitial: StateFlow<Boolean> = _showInterstitial.asStateFlow()

    // ── Per-session state ─────────────────────────────────────────────────────

    /** Correct-tap counter within the current try. Resets to 1 on each new try. */
    private var tapCount = 1

    /** Correct taps accumulated across ALL tries this session. */
    private var sessionCorrectTaps = 0

    /** Wrong taps accumulated across ALL tries this session (one per try used). */
    private var sessionWrongTaps = 0

    /** How many tries remain in the current 8-hour window. */
    private var triesRemaining = 0

    /** Total tries allowed in the current 8-hour window — set once on session start. */
    private var maxAttempts = 0

    private var currentDeltaE = ThresholdGameEngine.STARTING_DELTA_E
    private var oddIndex = 0
    private var baseColor: Color = Color.Unspecified

    /** Best (lowest) ΔE survived across all tries in this session. */
    private var bestDeltaE: Float? = null

    /**
     * Set to true when the session ends (all tries exhausted → navigate to result).
     * Checked in [onResume] so re-entering the screen always starts a fresh session
     * instead of resuming the stale wrong-tap state.
     */
    private var sessionEnded = false

    /** Stored all-time personal best at session start — used for eager-save comparison. */
    private var storedBestDeltaE: Float? = null

    /** Lifetime gem total (kept in sync with DB). */
    private var totalGems: Int = 0

    /** Gems earned in this session — shown on Result screen. */
    private var sessionGems: Int = 0

    /** Gems from correct taps (excluding milestones). */
    private var sessionTapGems: Int = 0

    /** Gems from milestone bonuses. */
    private var sessionMilestoneGems: Int = 0

    private var sessionLevelUpGems: Int = 0
    private var sessionLevelUpTo: PlayerLevel? = null
    private var isSessionNewPersonalBest = false

    /**
     * Milestones awarded in the *current try*.
     * Cleared on every wrong tap so each new try can earn milestones fresh.
     */
    private val awardedMilestones = mutableSetOf<Float>()

    /**
     * Increments on every [emitRound] call — correct AND wrong-tap-resets.
     * Drives [RadialSwatchLayout] unfold animation regardless of [tapCount].
     */
    private var roundGeneration = 0

    // Tracks last layout style to prevent same shape twice in a row.
    private var lastLayoutStyle: SwatchLayoutStyle? = null

    // Current player level — derived from gems on session start; drives hue exclusion.
    private var playerLevel: PlayerLevel = PlayerLevel.Rookie

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    init {
        safeLaunch { _isPaid.value = settingsRepository.isPaid() }
        loadGame()
    }

    /**
     * Called on every screen RESUME (including after config changes / rotation).
     *
     * Only re-checks attempt status when the screen is already showing [ThresholdUiState.Blocked]
     * — e.g. the user waited for the 8-hour cooldown to expire while the app was backgrounded.
     *
     * Active gameplay is never touched here: [viewModelScope] coroutines survive config changes,
     * so in-flight animations and state mutations continue uninterrupted without any help from
     * the screen lifecycle.
     */
    fun onResume() {
        // Also reload when the previous session ended (triesRemaining hit 0 and navigated to
        // result) — the ViewModel survives navigation so we need to explicitly start fresh.
        if (_uiState.value is ThresholdUiState.Blocked || sessionEnded) {
            sessionEnded = false
            loadGame()
        }
    }

    fun onInterstitialDone() {
        _showInterstitial.value = false
        safeLaunch { _navEvent.emit(ThresholdNavEvent.NavigateToResult) }
    }

    fun onUiEvent(event: ThresholdUiEvent) {
        when (event) {
            is ThresholdUiEvent.SwatchTapped -> handleSwatchTap(event.index)
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun loadGame() {
        safeLaunch {
            val now = Clock.System.now()
            when (val status = repository.getAttemptStatus(now)) {
                is AttemptStatus.Available -> startSession(status)
                is AttemptStatus.Exhausted -> _uiState.value = ThresholdUiState.Blocked(
                    nextResetAt = status.nextResetAt,
                    attemptsUsed = status.maxAttempts,
                    maxAttempts = status.maxAttempts,
                )
            }
        }
    }

    private suspend fun startSession(status: AttemptStatus.Available) {
        totalGems = settingsRepository.getGems()
        playerState.updateGems(totalGems)
        playerLevel = PlayerLevel.fromGems(totalGems)
        storedBestDeltaE = repository.getPersonalBest()?.bestDeltaE
        baseColor = colorEngine.randomVividColorExcluding(playerLevel.levelHue)
        currentDeltaE = ThresholdGameEngine.STARTING_DELTA_E
        tapCount = 1
        bestDeltaE = null
        sessionGems = 0
        sessionTapGems = 0
        sessionMilestoneGems = 0
        sessionLevelUpGems = 0
        sessionLevelUpTo = null
        sessionCorrectTaps = 0
        sessionWrongTaps = 0
        isSessionNewPersonalBest = false
        triesRemaining = status.maxAttempts - status.attemptsUsed
        maxAttempts = status.maxAttempts
        awardedMilestones.clear()
        println(
            "[DEBUG_DELTA] SESSION START — startingΔE=$currentDeltaE triesRemaining=$triesRemaining storedBestΔE=$storedBestDeltaE",
        )
        emitRound()
    }

    private fun emitRound() {
        val round = gameEngine.generateRound(baseColor, currentDeltaE)
        oddIndex = round.oddIndex
        roundGeneration++
        _uiState.value = ThresholdUiState.Playing(
            swatches = round.swatches.map { SwatchUiModel(it) },
            deltaE = currentDeltaE,
            tap = tapCount,
            attemptsRemaining = triesRemaining,
            maxAttempts = maxAttempts,
            roundPhase = RoundPhase.Idle,
            totalGems = totalGems,
            layoutStyle = pickLayoutStyle(),
            roundGeneration = roundGeneration,
            sessionBestDeltaE = bestDeltaE,
        )
    }

    private fun pickLayoutStyle(): SwatchLayoutStyle {
        val all = SwatchLayoutStyle.entries
        val candidates = if (lastLayoutStyle != null) all.filter { it != lastLayoutStyle } else all
        return candidates.random().also { lastLayoutStyle = it }
    }

    private fun handleSwatchTap(index: Int) {
        val state = _uiState.value as? ThresholdUiState.Playing ?: return
        if (state.roundPhase != RoundPhase.Idle) return
        if (index == oddIndex) handleCorrectTap(state) else handleWrongTap(state, tappedIndex = index)
    }

    private fun handleCorrectTap(state: ThresholdUiState.Playing) {
        val prevBestDeltaE = bestDeltaE
        bestDeltaE = bestDeltaE?.let { minOf(it, currentDeltaE) } ?: currentDeltaE

        val sessionBest = bestDeltaE!!
        val isNewAllTimeBest = storedBestDeltaE == null || sessionBest < storedBestDeltaE!!
        println(
            "[DEBUG_DELTA] CORRECT tap=$tapCount currentΔE=$currentDeltaE bestΔE(prev=$prevBestDeltaE → now=$sessionBest) isNewAllTimeBest=$isNewAllTimeBest",
        )
        if (isNewAllTimeBest) {
            storedBestDeltaE = sessionBest
            isSessionNewPersonalBest = true
        }

        val milestoneBonus = checkAndAwardMilestone(currentDeltaE)
        val gemsThisTap = GameRewardRates.THRESHOLD_CORRECT_TAP + milestoneBonus

        hapticEngine.perform(HapticType.CorrectTap)

        _uiState.value = state.copy(
            swatches = state.swatches.mapIndexed { i, s ->
                if (i == oddIndex) s.copy(displayState = SwatchDisplayState.Correct) else s
            },
            roundPhase = RoundPhase.Correct,
        )
        safeLaunch {
            // Milestone thud lands 200 ms after the CorrectTap snap — layered, not simultaneous.
            if (milestoneBonus > 0) {
                delay(MILESTONE_HAPTIC_DELAY_MS)
                hapticEngine.perform(HapticType.MilestoneHit)
            }
            // Save personal best FIRST, before any animation delays, using NonCancellable so
            // the DB write survives even if the user backs out and viewModelScope is cancelled.
            if (isNewAllTimeBest) {
                withContext(NonCancellable) {
                    repository.savePersonalBest(sessionBest)
                }
            }

            val levelBefore = PlayerLevel.fromGems(totalGems)
            totalGems = settingsRepository.addGems(gemsThisTap)
            sessionGems += gemsThisTap
            sessionTapGems += GameRewardRates.THRESHOLD_CORRECT_TAP
            sessionMilestoneGems += milestoneBonus

            // Level-up bonus
            val levelAfter = PlayerLevel.fromGems(totalGems)
            if (levelAfter.ordinal > levelBefore.ordinal) {
                sessionLevelUpTo = levelAfter
                val bonus = PlayerLevel.levelUpBonus(levelAfter)
                if (bonus > 0) {
                    totalGems = settingsRepository.addGems(bonus)
                    sessionGems += bonus
                    sessionLevelUpGems += bonus
                }
            }
            playerState.updateGems(totalGems)

            delay(ANIMATION_CORRECT_MS)
            (_uiState.value as? ThresholdUiState.Playing)?.let {
                _uiState.value = it.copy(roundPhase = RoundPhase.FoldingOut, totalGems = totalGems)
            }
            delay(ANIMATION_FOLD_MS)

            tapCount++
            val prevDeltaE = currentDeltaE
            currentDeltaE = (currentDeltaE - ThresholdGameEngine.DELTA_E_STEP)
                .coerceAtLeast(ThresholdGameEngine.MIN_DELTA_E)
            println(
                "[DEBUG_DELTA] NEXT ROUND tap=$tapCount ΔE: $prevDeltaE → $currentDeltaE (step=${ThresholdGameEngine.DELTA_E_STEP})",
            )
            baseColor = colorEngine.randomVividColorExcluding(playerLevel.levelHue)
            emitRound()
        }
    }

    private fun handleWrongTap(state: ThresholdUiState.Playing, tappedIndex: Int) {
        hapticEngine.perform(HapticType.WrongTap)

        val sting = wrongStingCopy(currentDeltaE)
        _uiState.value = state.copy(
            swatches = state.swatches.mapIndexed { i, s ->
                when (i) {
                    tappedIndex -> s.copy(displayState = SwatchDisplayState.Wrong)
                    oddIndex -> s.copy(displayState = SwatchDisplayState.Revealed)
                    else -> s
                }
            },
            roundPhase = RoundPhase.Wrong,
            stingCopy = sting,
        )
        safeLaunch {
            // Timeout so a slow/hanging DB write never permanently locks the game UI
            try {
                withTimeout(3_000L) { repository.recordAttempt(Clock.System.now()) }
            } catch (_: TimeoutCancellationException) {
                println("[DEBUG_DELTA] recordAttempt timed out — proceeding anyway")
            } catch (_: Exception) {
                println("[DEBUG_DELTA] recordAttempt failed — proceeding anyway")
            }
            triesRemaining--
            // Accumulate this try's stats before tapCount resets
            sessionCorrectTaps += tapCount - 1
            sessionWrongTaps++
            println(
                "[DEBUG_DELTA] WRONG failedAtΔE=$currentDeltaE tap=$tapCount bestΔE=$bestDeltaE triesRemaining=$triesRemaining sessionCorrect=$sessionCorrectTaps sessionWrong=$sessionWrongTaps",
            )

            delay(ANIMATION_WRONG_MS)

            if (triesRemaining > 0) {
                // Still has tries — reset ΔE and milestones for the new try
                awardedMilestones.clear()
                (_uiState.value as? ThresholdUiState.Playing)?.let {
                    _uiState.value = it.copy(roundPhase = RoundPhase.FoldingOut)
                }
                delay(ANIMATION_FOLD_MS)
                currentDeltaE = ThresholdGameEngine.STARTING_DELTA_E
                tapCount = 1
                println("[DEBUG_DELTA] NEW TRY — reset ΔE=$currentDeltaE triesRemaining=$triesRemaining")
                baseColor = colorEngine.randomVividColorExcluding(playerLevel.levelHue)
                emitRound()
            } else {
                // All tries spent — navigate to result
                hapticEngine.perform(HapticType.GameOver)
                val finalDeltaE = bestDeltaE ?: currentDeltaE
                println(
                    "[DEBUG_DELTA] SESSION END — finalΔE=$finalDeltaE bestΔE=$bestDeltaE fallback(currentΔE)=${bestDeltaE == null} correctRounds=$sessionCorrectTaps totalRounds=${sessionCorrectTaps + sessionWrongTaps}",
                )
                repository.savePersonalBest(finalDeltaE)
                val breakdown = buildList {
                    if (sessionTapGems > 0) add(GemAward("Correct taps", sessionTapGems))
                    if (sessionMilestoneGems > 0) add(GemAward("Milestone bonuses", sessionMilestoneGems))
                    if (sessionLevelUpGems > 0) add(GemAward("Level up bonus", sessionLevelUpGems))
                }
                sessionResultCache.set(
                    SessionResult(
                        gameId = GameId.THRESHOLD,
                        deltaE = finalDeltaE,
                        roundsSurvived = tapCount - 1,
                        correctRounds = sessionCorrectTaps,
                        totalRounds = sessionCorrectTaps + sessionWrongTaps,
                        gemsEarned = sessionGems,
                        gemBreakdown = breakdown,
                        levelUpTo = sessionLevelUpTo,
                    ),
                )

                // Mark session done before navigating so onResume() starts fresh
                // when the user re-enters ThresholdScreen after viewing the result.
                sessionEnded = true

                // Show interstitial (free users only, every 2nd session, never after personal best)
                if (!_isPaid.value) {
                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    if (adOrchestrator.shouldShowInterstitial(isSessionNewPersonalBest, today)) {
                        adOrchestrator.onInterstitialShown()
                        _showInterstitial.value = true
                        return@launch // screen calls onInterstitialDone() → navigates
                    }
                }

                _navEvent.emit(ThresholdNavEvent.NavigateToResult)
            }
        }
    }

    /**
     * Checks if [deltaE] crosses a milestone boundary for the first time in the current try.
     * Awards the bonus and records the milestone so it isn't double-awarded.
     *
     * @return gems to award (0 if no new milestone crossed).
     */
    private fun checkAndAwardMilestone(deltaE: Float): Int {
        for ((boundary, bonus) in GameRewardRates.THRESHOLD_MILESTONES) {
            if (deltaE < boundary && awardedMilestones.add(boundary)) {
                return bonus
            }
        }
        return 0
    }

    private fun wrongStingCopy(deltaE: Float): String = when {
        deltaE > 4f -> "Too easy. You missed anyway."
        deltaE > 3f -> "Your eyes chose... wrong."
        deltaE > 2f -> "So close. And yet."
        deltaE > 1f -> "You had it. Your brain didn't."
        deltaE > 0.5f -> "Elite miss. That stings, right?"
        else -> "Superhuman territory. And you fumbled."
    }

    private companion object {
        const val ANIMATION_CORRECT_MS = 750L
        const val ANIMATION_WRONG_MS = 850L
        const val ANIMATION_FOLD_MS = 520L
        const val MILESTONE_HAPTIC_DELAY_MS = 200L
    }
}
