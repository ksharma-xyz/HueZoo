package xyz.ksharma.huezoo.ui.games.threshold

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.data.repository.ThresholdRepository
import xyz.ksharma.huezoo.domain.color.ColorEngine
import xyz.ksharma.huezoo.domain.game.GameRewardRates
import xyz.ksharma.huezoo.domain.game.ThresholdGameEngine
import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.GemAward
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdNavEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiState
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.model.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.model.SwatchLayoutStyle
import xyz.ksharma.huezoo.ui.model.SwatchUiModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ThresholdViewModel(
    private val gameEngine: ThresholdGameEngine,
    private val repository: ThresholdRepository,
    private val colorEngine: ColorEngine,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ThresholdUiState>(ThresholdUiState.Loading)
    val uiState: StateFlow<ThresholdUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ThresholdNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<ThresholdNavEvent> = _navEvent.asSharedFlow()

    // ── Per-session state ─────────────────────────────────────────────────────

    /** Correct-tap counter within the current try. Resets to 1 on each new try. */
    private var tapCount = 1

    /** Correct taps accumulated across ALL tries this session. */
    private var sessionCorrectTaps = 0

    /** Wrong taps accumulated across ALL tries this session (one per try used). */
    private var sessionWrongTaps = 0

    /** How many tries remain in the current 8-hour window. */
    private var triesRemaining = 0

    private var currentDeltaE = ThresholdGameEngine.STARTING_DELTA_E
    private var oddIndex = 0
    private var baseColor: Color = Color.Unspecified

    /** Best (lowest) ΔE survived across all tries in this session. */
    private var bestDeltaE: Float? = null

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
        loadGame()
    }

    fun onStart() {
        when (val current = _uiState.value) {
            is ThresholdUiState.Playing -> if (current.roundPhase != RoundPhase.Idle) loadGame()
            is ThresholdUiState.Blocked -> loadGame()
            else -> Unit
        }
    }

    fun onUiEvent(event: ThresholdUiEvent) {
        when (event) {
            is ThresholdUiEvent.SwatchTapped -> handleSwatchTap(event.index)
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun loadGame() {
        viewModelScope.launch {
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
        playerLevel = PlayerLevel.fromGems(totalGems)
        storedBestDeltaE = repository.getPersonalBest()?.bestDeltaE
        baseColor = colorEngine.randomVividColorExcluding(playerLevel.levelHue)
        currentDeltaE = ThresholdGameEngine.STARTING_DELTA_E
        tapCount = 1
        bestDeltaE = null
        sessionGems = 0
        sessionTapGems = 0
        sessionMilestoneGems = 0
        sessionCorrectTaps = 0
        sessionWrongTaps = 0
        triesRemaining = status.maxAttempts - status.attemptsUsed
        awardedMilestones.clear()
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
            roundPhase = RoundPhase.Idle,
            totalGems = totalGems,
            layoutStyle = pickLayoutStyle(),
            roundGeneration = roundGeneration,
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
        bestDeltaE = bestDeltaE?.let { minOf(it, currentDeltaE) } ?: currentDeltaE

        val sessionBest = bestDeltaE!!
        val isNewAllTimeBest = storedBestDeltaE == null || sessionBest < storedBestDeltaE!!
        if (isNewAllTimeBest) storedBestDeltaE = sessionBest

        val milestoneBonus = checkAndAwardMilestone(currentDeltaE)
        val gemsThisTap = GameRewardRates.THRESHOLD_CORRECT_TAP + milestoneBonus

        _uiState.value = state.copy(
            swatches = state.swatches.mapIndexed { i, s ->
                if (i == oddIndex) s.copy(displayState = SwatchDisplayState.Correct) else s
            },
            roundPhase = RoundPhase.Correct,
        )
        viewModelScope.launch {
            // Save personal best FIRST, before any animation delays, using NonCancellable so
            // the DB write survives even if the user backs out and viewModelScope is cancelled.
            if (isNewAllTimeBest) {
                withContext(NonCancellable) {
                    repository.savePersonalBest(sessionBest)
                }
            }

            totalGems = settingsRepository.addGems(gemsThisTap)
            sessionGems += gemsThisTap
            sessionTapGems += GameRewardRates.THRESHOLD_CORRECT_TAP
            sessionMilestoneGems += milestoneBonus

            delay(ANIMATION_CORRECT_MS)
            (_uiState.value as? ThresholdUiState.Playing)?.let {
                _uiState.value = it.copy(roundPhase = RoundPhase.FoldingOut, totalGems = totalGems)
            }
            delay(ANIMATION_FOLD_MS)

            tapCount++
            currentDeltaE = (currentDeltaE - ThresholdGameEngine.DELTA_E_STEP)
                .coerceAtLeast(ThresholdGameEngine.MIN_DELTA_E)
            baseColor = colorEngine.randomVividColorExcluding(playerLevel.levelHue)
            emitRound()
        }
    }

    private fun handleWrongTap(state: ThresholdUiState.Playing, tappedIndex: Int) {
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
        // Capture before the launch — currentDeltaE is the level the player just attempted.
        // If they've correctly tapped at least once (bestDeltaE != null), reaching this level
        // counts toward personal best even if they failed the round. Guards against saving
        // the trivial 5.0 starting value when someone taps wrong with no correct taps at all.
        val attemptedDeltaE = currentDeltaE
        val isReachPersonalBest = bestDeltaE != null &&
            (storedBestDeltaE == null || attemptedDeltaE < storedBestDeltaE!!)
        if (isReachPersonalBest) storedBestDeltaE = attemptedDeltaE

        viewModelScope.launch {
            // Persist "reached" personal best before any delay — NonCancellable so a back-press
            // mid-animation doesn't lose the write.
            if (isReachPersonalBest) {
                withContext(NonCancellable) {
                    repository.savePersonalBest(attemptedDeltaE)
                }
            }

            repository.recordAttempt(Clock.System.now())
            triesRemaining--
            // Accumulate this try's stats before tapCount resets
            sessionCorrectTaps += tapCount - 1
            sessionWrongTaps++

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
                baseColor = colorEngine.randomVividColorExcluding(playerLevel.levelHue)
                emitRound()
            } else {
                // All tries spent — navigate to result
                val finalDeltaE = bestDeltaE ?: currentDeltaE
                repository.savePersonalBest(finalDeltaE)
                val breakdown = buildList {
                    if (sessionTapGems > 0) add(GemAward("Correct taps", sessionTapGems))
                    if (sessionMilestoneGems > 0) add(GemAward("Milestone bonuses", sessionMilestoneGems))
                }
                _navEvent.emit(
                    ThresholdNavEvent.NavigateToResult(
                        Result(
                            gameId = GameId.THRESHOLD,
                            deltaE = finalDeltaE,
                            roundsSurvived = tapCount - 1,
                            correctRounds = sessionCorrectTaps,
                            totalRounds = sessionCorrectTaps + sessionWrongTaps,
                            gemsEarned = sessionGems,
                            gemBreakdown = breakdown,
                        ),
                    ),
                )
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
    }
}
