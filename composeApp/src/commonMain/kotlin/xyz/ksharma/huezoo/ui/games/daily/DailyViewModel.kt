package xyz.ksharma.huezoo.ui.games.daily

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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.huezoo.data.repository.DailyRepository
import xyz.ksharma.huezoo.domain.color.ColorEngine
import xyz.ksharma.huezoo.domain.game.DailyGameEngine
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.Result
import xyz.ksharma.huezoo.ui.games.daily.state.DailyNavEvent
import xyz.ksharma.huezoo.ui.games.daily.state.DailyUiEvent
import xyz.ksharma.huezoo.ui.games.daily.state.DailyUiState
import xyz.ksharma.huezoo.ui.model.RoundPhase
import xyz.ksharma.huezoo.ui.model.SwatchDisplayState
import xyz.ksharma.huezoo.ui.model.SwatchLayoutStyle
import xyz.ksharma.huezoo.ui.model.SwatchUiModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DailyViewModel(
    private val gameEngine: DailyGameEngine,
    private val repository: DailyRepository,
    private val colorEngine: ColorEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyUiState>(DailyUiState.Loading)
    val uiState: StateFlow<DailyUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<DailyNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<DailyNavEvent> = _navEvent.asSharedFlow()

    private val today get() = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date

    private var roundIndex = 0
    private var oddIndex = 0
    private var cumulativeScore = 0
    private var lastRoundDeltaE = 0f
    private var roundGeneration = 0
    private var lastLayoutStyle: SwatchLayoutStyle? = null

    init {
        loadGame()
    }

    /**
     * Call when the screen enters composition (LaunchedEffect(Unit)).
     * - Re-checks DB when [AlreadyPlayed] in case a debug reset cleared the record.
     * - Restarts when a game ended mid-session (roundPhase != Idle).
     */
    fun onStart() {
        when (val current = _uiState.value) {
            is DailyUiState.AlreadyPlayed -> loadGame()
            is DailyUiState.Playing -> if (current.roundPhase != RoundPhase.Idle) loadGame()
            else -> Unit
        }
    }

    fun onUiEvent(event: DailyUiEvent) {
        when (event) {
            is DailyUiEvent.SwatchTapped -> handleSwatchTap(event.index)
        }
    }

    private fun loadGame() {
        // Reset all in-memory state before starting fresh.
        roundIndex = 0
        oddIndex = 0
        cumulativeScore = 0
        lastRoundDeltaE = 0f
        roundGeneration = 0
        lastLayoutStyle = null

        viewModelScope.launch {
            val date = today
            val existing = repository.getChallenge(date)
            if (existing?.completed == true) {
                _uiState.value = DailyUiState.AlreadyPlayed(score = existing.score)
            } else {
                val baseColor = colorEngine.seededColorForDate(date)
                emitRound(baseColor)
            }
        }
    }

    private fun emitRound(baseColor: androidx.compose.ui.graphics.Color) {
        val date = today
        val round = gameEngine.generateRound(date, roundIndex, baseColor)
        oddIndex = round.oddIndex
        lastRoundDeltaE = round.deltaE
        roundGeneration++
        _uiState.value = DailyUiState.Playing(
            swatches = round.swatches.map { SwatchUiModel(it) },
            deltaE = round.deltaE,
            round = roundIndex + 1,
            totalRounds = gameEngine.totalRounds,
            roundPhase = RoundPhase.Idle,
            layoutStyle = pickLayoutStyle(),
            roundGeneration = roundGeneration,
        )
    }

    /**
     * Returns a random [SwatchLayoutStyle] that is different from [lastLayoutStyle],
     * so the same shape never appears in two consecutive rounds.
     */
    private fun pickLayoutStyle(): SwatchLayoutStyle {
        val all = SwatchLayoutStyle.entries
        val candidates = if (lastLayoutStyle != null) all.filter { it != lastLayoutStyle } else all
        return candidates.random().also { lastLayoutStyle = it }
    }

    private fun handleSwatchTap(index: Int) {
        val state = _uiState.value as? DailyUiState.Playing ?: return
        if (state.roundPhase != RoundPhase.Idle) return

        if (index == oddIndex) {
            handleCorrectTap(state)
        } else {
            handleWrongTap(state, tappedIndex = index)
        }
    }

    private fun handleCorrectTap(state: DailyUiState.Playing) {
        cumulativeScore += colorEngine.scoreFromDeltaE(lastRoundDeltaE)
        _uiState.value = state.copy(
            swatches = state.swatches.mapIndexed { i, s ->
                if (i == oddIndex) s.copy(displayState = SwatchDisplayState.Correct) else s
            },
            roundPhase = RoundPhase.Correct,
        )
        viewModelScope.launch {
            delay(ANIMATION_CORRECT_MS)
            val isLastRound = roundIndex == gameEngine.totalRounds - 1
            if (isLastRound) {
                finishGame()
            } else {
                // Fold the flower before advancing to next round.
                (_uiState.value as? DailyUiState.Playing)?.let {
                    _uiState.value = it.copy(roundPhase = RoundPhase.FoldingOut)
                }
                delay(ANIMATION_FOLD_MS)
                roundIndex++
                val baseColor = colorEngine.seededColorForDate(today)
                emitRound(baseColor)
            }
        }
    }

    private fun handleWrongTap(state: DailyUiState.Playing, tappedIndex: Int) {
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
            finishGame()
        }
    }

    private suspend fun finishGame() {
        val date = today
        val score = cumulativeScore.toFloat()
        repository.saveCompletion(date, score)
        repository.savePersonalBest(lastRoundDeltaE, cumulativeScore)
        _navEvent.emit(
            DailyNavEvent.NavigateToResult(
                Result(
                    gameId = GameId.DAILY,
                    deltaE = lastRoundDeltaE,
                    roundsSurvived = roundIndex,
                    score = cumulativeScore,
                ),
            ),
        )
    }

    private companion object {
        const val ANIMATION_CORRECT_MS = 350L
        const val ANIMATION_WRONG_MS = 850L
        /** Time budget for the flower fold-out animation before the next round is emitted. */
        const val ANIMATION_FOLD_MS = 520L
    }
}
