package xyz.ksharma.huezoo.ui.games.threshold

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import xyz.ksharma.huezoo.data.repository.ThresholdRepository
import xyz.ksharma.huezoo.domain.color.ColorEngine
import xyz.ksharma.huezoo.domain.game.ThresholdGameEngine
import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.games.threshold.state.RoundPhase
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdNavEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiEvent
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiState
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.model.SwatchUiModel

class ThresholdViewModel(
    private val gameEngine: ThresholdGameEngine,
    private val repository: ThresholdRepository,
    private val colorEngine: ColorEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ThresholdUiState>(ThresholdUiState.Loading)
    val uiState: StateFlow<ThresholdUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ThresholdNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<ThresholdNavEvent> = _navEvent.asSharedFlow()

    // Mutable game state — not exposed directly in UiState to keep state atomic.
    private var currentDeltaE = ThresholdGameEngine.STARTING_DELTA_E
    private var roundCount = 1
    private var attemptsRemaining = ThresholdGameEngine.MAX_ATTEMPTS
    private var oddIndex = 0
    private var lastSuccessfulDeltaE: Float? = null
    private var baseColor: Color = Color.Unspecified

    init {
        loadGame()
    }

    /**
     * Call when the screen enters composition (LaunchedEffect(Unit)).
     * If the ViewModel is being reused after a completed game (roundPhase != Idle),
     * start a fresh game rather than leaving the UI stuck on the end state.
     */
    fun onStart() {
        val current = _uiState.value
        if (current is ThresholdUiState.Playing && current.roundPhase != RoundPhase.Idle) {
            loadGame()
        }
    }

    fun onUiEvent(event: ThresholdUiEvent) {
        when (event) {
            is ThresholdUiEvent.SwatchTapped -> handleSwatchTap(event.index)
        }
    }

    private fun loadGame() {
        viewModelScope.launch {
            val now = Clock.System.now()
            when (val status = repository.getAttemptStatus(now)) {
                is AttemptStatus.Available -> startGame(status)
                is AttemptStatus.Exhausted -> _uiState.value = ThresholdUiState.Blocked(
                    nextResetAt = status.nextResetAt,
                    attemptsUsed = ThresholdGameEngine.MAX_ATTEMPTS,
                    maxAttempts = ThresholdGameEngine.MAX_ATTEMPTS,
                )
            }
        }
    }

    private suspend fun startGame(status: AttemptStatus.Available) {
        repository.recordAttempt(Clock.System.now())
        baseColor = colorEngine.randomVividColor()
        currentDeltaE = ThresholdGameEngine.STARTING_DELTA_E
        roundCount = 1
        lastSuccessfulDeltaE = null
        attemptsRemaining = status.maxAttempts - (status.attemptsUsed + 1)
        emitRound()
    }

    private fun emitRound() {
        val round = gameEngine.generateRound(baseColor, currentDeltaE)
        oddIndex = round.oddIndex
        _uiState.value = ThresholdUiState.Playing(
            swatches = round.swatches.map { SwatchUiModel(it) },
            deltaE = currentDeltaE,
            round = roundCount,
            attemptsRemaining = attemptsRemaining,
            roundPhase = RoundPhase.Idle,
        )
    }

    private fun handleSwatchTap(index: Int) {
        val state = _uiState.value as? ThresholdUiState.Playing ?: return
        if (state.roundPhase != RoundPhase.Idle) return

        if (index == oddIndex) {
            handleCorrectTap(state)
        } else {
            handleWrongTap(state, tappedIndex = index)
        }
    }

    private fun handleCorrectTap(state: ThresholdUiState.Playing) {
        lastSuccessfulDeltaE = currentDeltaE
        _uiState.value = state.copy(
            swatches = state.swatches.mapIndexed { i, s ->
                if (i == oddIndex) s.copy(displayState = SwatchDisplayState.Correct) else s
            },
            roundPhase = RoundPhase.Correct,
        )
        viewModelScope.launch {
            delay(ANIMATION_CORRECT_MS)
            roundCount++
            currentDeltaE = (currentDeltaE - ThresholdGameEngine.DELTA_E_STEP)
                .coerceAtLeast(ThresholdGameEngine.MIN_DELTA_E)
            emitRound()
        }
    }

    private fun handleWrongTap(state: ThresholdUiState.Playing, tappedIndex: Int) {
        _uiState.value = state.copy(
            swatches = state.swatches.mapIndexed { i, s ->
                when (i) {
                    tappedIndex -> s.copy(displayState = SwatchDisplayState.Wrong)
                    oddIndex -> s.copy(displayState = SwatchDisplayState.Revealed)
                    else -> s
                }
            },
            roundPhase = RoundPhase.Wrong,
        )
        viewModelScope.launch {
            delay(ANIMATION_WRONG_MS)
            val score = lastSuccessfulDeltaE
                ?.let { colorEngine.scoreFromDeltaE(it) }
                ?: 0
            val roundsSurvived = roundCount - 1
            repository.savePersonalBest(currentDeltaE, score)
            _navEvent.emit(
                ThresholdNavEvent.NavigateToResult(
                    Result(
                        gameId = GameId.THRESHOLD,
                        deltaE = currentDeltaE,
                        roundsSurvived = roundsSurvived,
                        score = score,
                    ),
                ),
            )
        }
    }

    private companion object {
        const val ANIMATION_CORRECT_MS = 350L
        const val ANIMATION_WRONG_MS = 450L
    }
}
