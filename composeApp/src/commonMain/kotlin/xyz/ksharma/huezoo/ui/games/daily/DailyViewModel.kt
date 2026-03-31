package xyz.ksharma.huezoo.ui.games.daily

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.huezoo.data.repository.DailyRepository
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.domain.color.ColorEngine
import xyz.ksharma.huezoo.domain.game.DailyGameEngine
import xyz.ksharma.huezoo.domain.game.GameRewardRates
import xyz.ksharma.huezoo.domain.game.SessionResultCache
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.GemAward
import xyz.ksharma.huezoo.navigation.SessionResult
import xyz.ksharma.huezoo.platform.haptics.HapticEngine
import xyz.ksharma.huezoo.platform.haptics.HapticType
import xyz.ksharma.huezoo.ui.games.daily.state.DailyNavEvent
import xyz.ksharma.huezoo.ui.games.daily.state.DailyUiEvent
import xyz.ksharma.huezoo.ui.games.daily.state.DailyUiState
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
class DailyViewModel(
    private val gameEngine: DailyGameEngine,
    private val repository: DailyRepository,
    private val settingsRepository: SettingsRepository,
    private val colorEngine: ColorEngine,
    private val playerState: PlayerState,
    private val hapticEngine: HapticEngine,
    private val sessionResultCache: SessionResultCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyUiState>(DailyUiState.Loading)
    val uiState: StateFlow<DailyUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<DailyNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<DailyNavEvent> = _navEvent.asSharedFlow()

    private val _isPaid = MutableStateFlow(false)
    val isPaid: StateFlow<Boolean> = _isPaid.asStateFlow()

    private val today get() = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date

    // ── Per-session state ─────────────────────────────────────────────────────

    private var roundIndex = 0
    private var oddIndex = 0

    /** Number of rounds answered correctly (0–6). */
    private var correctRounds = 0

    /** ΔE of the last round generated (used to evaluate the current round). */
    private var lastRoundDeltaE = 0f

    /** Highest ΔE among rounds the player answered correctly (shown in result / personal best). */
    private var highestCorrectDeltaE = 0f

    private var totalGems = 0
    private var sessionGems = 0
    private var sessionCorrectGems = 0
    private var sessionLevelUpGems = 0
    private var sessionLevelUpTo: PlayerLevel? = null

    private var roundGeneration = 0
    private var lastLayoutStyle: SwatchLayoutStyle? = null

    // Current player level — loaded on game start; drives hue exclusion for base colors.
    private var playerLevel: PlayerLevel = PlayerLevel.Rookie

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    init {
        loadGame()
    }

    /**
     * Called on every screen RESUME (including after config changes / rotation).
     *
     * Only re-checks today's challenge status when already showing [DailyUiState.AlreadyPlayed]
     * — e.g. the user completed the daily, went to another app, and returned (day may have changed).
     *
     * Active gameplay is never touched here: [viewModelScope] coroutines survive config changes,
     * so round state and animations continue uninterrupted.
     */
    fun onResume() {
        if (_uiState.value is DailyUiState.AlreadyPlayed) loadGame()
    }

    fun onUiEvent(event: DailyUiEvent) {
        when (event) {
            is DailyUiEvent.SwatchTapped -> handleSwatchTap(event.index)
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun loadGame() {
        roundIndex = 0
        oddIndex = 0
        correctRounds = 0
        lastRoundDeltaE = 0f
        highestCorrectDeltaE = 0f
        totalGems = 0
        sessionGems = 0
        sessionCorrectGems = 0
        sessionLevelUpGems = 0
        sessionLevelUpTo = null
        roundGeneration = 0
        lastLayoutStyle = null

        safeLaunch {
            _isPaid.value = settingsRepository.isPaid()
            val date = today
            val existing = repository.getChallenge(date)
            if (existing?.completed == true) {
                _uiState.value = DailyUiState.AlreadyPlayed
            } else {
                totalGems = settingsRepository.getGems()
                playerState.updateGems(totalGems)
                playerLevel = PlayerLevel.fromGems(totalGems)
                emitRound(colorEngine.randomVividColorExcluding(playerLevel.levelHue))
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

    private fun pickLayoutStyle(): SwatchLayoutStyle {
        val all = SwatchLayoutStyle.entries
        val candidates = if (lastLayoutStyle != null) all.filter { it != lastLayoutStyle } else all
        return candidates.random().also { lastLayoutStyle = it }
    }

    private fun handleSwatchTap(index: Int) {
        val state = _uiState.value as? DailyUiState.Playing ?: return
        if (state.roundPhase != RoundPhase.Idle) return
        if (index == oddIndex) handleCorrectTap(state) else handleWrongTap(state, tappedIndex = index)
    }

    private fun handleCorrectTap(state: DailyUiState.Playing) {
        hapticEngine.perform(HapticType.CorrectTap)

        correctRounds++
        if (lastRoundDeltaE > highestCorrectDeltaE) highestCorrectDeltaE = lastRoundDeltaE

        _uiState.value = state.copy(
            swatches = state.swatches.mapIndexed { i, s ->
                if (i == oddIndex) s.copy(displayState = SwatchDisplayState.Correct) else s
            },
            roundPhase = RoundPhase.Correct,
        )
        safeLaunch {
            val levelBefore = PlayerLevel.fromGems(totalGems)
            totalGems = settingsRepository.addGems(GameRewardRates.DAILY_CORRECT_ROUND)
            sessionGems += GameRewardRates.DAILY_CORRECT_ROUND
            sessionCorrectGems += GameRewardRates.DAILY_CORRECT_ROUND

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
            val isLastRound = roundIndex == gameEngine.totalRounds - 1
            if (isLastRound) {
                finishGame()
            } else {
                (_uiState.value as? DailyUiState.Playing)?.let {
                    _uiState.value = it.copy(roundPhase = RoundPhase.FoldingOut)
                }
                delay(ANIMATION_FOLD_MS)
                roundIndex++
                emitRound(colorEngine.randomVividColorExcluding(playerLevel.levelHue))
            }
        }
    }

    private fun handleWrongTap(state: DailyUiState.Playing, tappedIndex: Int) {
        hapticEngine.perform(HapticType.WrongTap)

        // Wrong tap: reveal correct swatch, show feedback, then advance to next round.
        // Daily never ends early — all 6 rounds are always played.
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
        safeLaunch {
            delay(ANIMATION_WRONG_MS)
            val isLastRound = roundIndex == gameEngine.totalRounds - 1
            if (isLastRound) {
                finishGame()
            } else {
                (_uiState.value as? DailyUiState.Playing)?.let {
                    _uiState.value = it.copy(roundPhase = RoundPhase.FoldingOut)
                }
                delay(ANIMATION_FOLD_MS)
                roundIndex++
                emitRound(colorEngine.randomVividColorExcluding(playerLevel.levelHue))
            }
        }
    }

    private suspend fun finishGame() {
        val date = today

        // Participation bonus — always awarded for completing all 6 rounds
        totalGems = settingsRepository.addGems(GameRewardRates.DAILY_PARTICIPATION)
        sessionGems += GameRewardRates.DAILY_PARTICIPATION

        // Perfect bonus — all 6 rounds correct
        val isPerfect = correctRounds == gameEngine.totalRounds
        if (isPerfect) {
            hapticEngine.perform(HapticType.PerfectRun)
            totalGems = settingsRepository.addGems(GameRewardRates.DAILY_PERFECT_BONUS)
            sessionGems += GameRewardRates.DAILY_PERFECT_BONUS
        }

        // Save completion first so streak includes today
        repository.saveCompletion(date)

        // Streak bonus
        val streak = repository.getStreak(date)
        val streakBonus = when {
            streak >= STREAK_THRESHOLD_7 -> STREAK_BONUS_7
            streak >= STREAK_THRESHOLD_3 -> STREAK_BONUS_3
            else -> 0
        }
        if (streakBonus > 0) {
            totalGems = settingsRepository.addGems(streakBonus)
            sessionGems += streakBonus
        }

        playerState.updateGems(totalGems)
        repository.savePersonalBest(highestCorrectDeltaE, correctRounds)

        val breakdown = buildList {
            if (sessionCorrectGems > 0) add(GemAward("Correct rounds ×$correctRounds", sessionCorrectGems))
            add(GemAward("Participation", GameRewardRates.DAILY_PARTICIPATION))
            if (isPerfect) add(GemAward("Perfect run bonus", GameRewardRates.DAILY_PERFECT_BONUS))
            if (streakBonus > 0) add(GemAward("Streak ×$streak bonus", streakBonus))
            if (sessionLevelUpGems > 0) add(GemAward("Level up bonus", sessionLevelUpGems))
        }
        sessionResultCache.set(
            SessionResult(
                gameId = GameId.DAILY,
                deltaE = highestCorrectDeltaE,
                roundsSurvived = correctRounds,
                correctRounds = correctRounds,
                totalRounds = gameEngine.totalRounds,
                gemsEarned = sessionGems,
                gemBreakdown = breakdown,
                levelUpTo = sessionLevelUpTo,
            ),
        )
        _navEvent.emit(DailyNavEvent.NavigateToResult)
    }

    private companion object {
        const val ANIMATION_CORRECT_MS = 750L
        const val ANIMATION_WRONG_MS = 850L
        const val ANIMATION_FOLD_MS = 520L
        const val STREAK_THRESHOLD_3 = 3
        const val STREAK_THRESHOLD_7 = 7
        const val STREAK_BONUS_3 = 5
        const val STREAK_BONUS_7 = 10
    }
}
