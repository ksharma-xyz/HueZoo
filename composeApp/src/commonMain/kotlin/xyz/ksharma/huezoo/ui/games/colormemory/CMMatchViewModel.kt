package xyz.ksharma.huezoo.ui.games.colormemory

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.ksharma.huezoo.data.repository.ColorMemoryRepository
import xyz.ksharma.huezoo.data.repository.SettingsRepository
import xyz.ksharma.huezoo.domain.color.ColorPair
import xyz.ksharma.huezoo.domain.game.ColorMemoryGameEngine
import xyz.ksharma.huezoo.domain.game.ColorMemoryRewards
import xyz.ksharma.huezoo.domain.game.SessionResultCache
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.GemAward
import xyz.ksharma.huezoo.navigation.SessionResult
import xyz.ksharma.huezoo.platform.haptics.HapticEngine
import xyz.ksharma.huezoo.platform.haptics.HapticType
import xyz.ksharma.huezoo.ui.copy.CMMatchStingCopy
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMLastAnswer
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMRoundResult
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMatchNavEvent
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMatchPhase
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMatchUiEvent
import xyz.ksharma.huezoo.ui.games.colormemory.state.CMMatchUiState
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.model.PlayerState
import xyz.ksharma.huezoo.ui.util.safeLaunch

class CMMatchViewModel(
    private val gameEngine: ColorMemoryGameEngine,
    private val colorMemoryRepository: ColorMemoryRepository,
    private val settingsRepository: SettingsRepository,
    private val playerState: PlayerState,
    private val hapticEngine: HapticEngine,
    private val sessionResultCache: SessionResultCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CMMatchUiState>(CMMatchUiState.Loading)
    val uiState: StateFlow<CMMatchUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<CMMatchNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<CMMatchNavEvent> = _navEvent.asSharedFlow()

    private val _isPaid = MutableStateFlow(false)
    val isPaid: StateFlow<Boolean> = _isPaid.asStateFlow()

    // ── Per-session state ─────────────────────────────────────────────────────

    private var round = 1
    private var score = 0
    private var currentPair: ColorPair? = null
    private val roundResults = mutableListOf<Boolean>()
    private var tightestCorrectDeltaE: Float? = null
    private var roundGeneration = 0
    private var totalGems = 0

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    init {
        startSession()
    }

    fun onUiEvent(event: CMMatchUiEvent) {
        when (event) {
            is CMMatchUiEvent.Answer -> handleAnswer(event.saidSame)
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun startSession() {
        round = 1
        score = 0
        currentPair = null
        roundResults.clear()
        tightestCorrectDeltaE = null
        roundGeneration = 0

        safeLaunch {
            _isPaid.value = settingsRepository.isPaid()
            totalGems = settingsRepository.getGems()
            playerState.updateGems(totalGems)
            startRound()
        }
    }

    /**
     * Emits a fresh round in [CMMatchPhase.Memory] and drives the timed phases:
     * Memory (3000 ms) → Hold (400 ms) → Recall (until the player answers).
     */
    private suspend fun startRound() {
        val pair = gameEngine.generateRound(round)
        currentPair = pair
        roundGeneration++
        val generation = roundGeneration

        _uiState.value = CMMatchUiState.Playing(
            round = round,
            totalRounds = gameEngine.totalRounds,
            phase = CMMatchPhase.Memory,
            score = score,
            roundResults = roundResults.map { if (it) CMMRoundResult.Correct else CMMRoundResult.Wrong },
            colorA = pair.a,
            colorB = pair.b,
            currentDeltaE = pair.deltaE,
            roundDeltaE = gameEngine.deltaECurve[round - 1],
            lastAnswer = null,
            roundGeneration = generation,
        )

        delay(MEMORY_MS)
        advancePhase(generation, CMMatchPhase.Hold)
        delay(HOLD_MS)
        advancePhase(generation, CMMatchPhase.Recall)
    }

    /** Moves to [phase] only if the state still belongs to [generation] — guards stale timers. */
    private fun advancePhase(generation: Int, phase: CMMatchPhase) {
        val state = _uiState.value as? CMMatchUiState.Playing ?: return
        if (state.roundGeneration != generation) return
        _uiState.value = state.copy(phase = phase)
    }

    private fun handleAnswer(saidSame: Boolean) {
        val state = _uiState.value as? CMMatchUiState.Playing ?: return
        if (state.phase != CMMatchPhase.Recall) return
        val pair = currentPair ?: return

        val correct = saidSame == pair.isSame
        hapticEngine.perform(if (correct) HapticType.CorrectTap else HapticType.WrongTap)

        roundResults += correct
        score += if (correct) ColorMemoryGameEngine.POINTS_CORRECT else ColorMemoryGameEngine.POINTS_WRONG
        if (correct && !pair.isSame) {
            val current = tightestCorrectDeltaE
            if (current == null || pair.deltaE < current) tightestCorrectDeltaE = pair.deltaE
        }

        _uiState.value = state.copy(
            phase = CMMatchPhase.Feedback,
            score = score,
            roundResults = roundResults.map { if (it) CMMRoundResult.Correct else CMMRoundResult.Wrong },
            lastAnswer = CMMLastAnswer(
                correct = correct,
                truthSame = pair.isSame,
                deltaE = state.roundDeltaE,
                sting = CMMatchStingCopy.forRound(correct = correct, deltaE = state.roundDeltaE),
            ),
        )

        safeLaunch {
            delay(FEEDBACK_MS)
            if (round == gameEngine.totalRounds) {
                finishGame()
            } else {
                round++
                startRound()
            }
        }
    }

    private suspend fun finishGame() {
        val payout = ColorMemoryRewards.calculate(roundResults)
        val correctCount = roundResults.count { it }
        val isPerfect = correctCount == gameEngine.totalRounds
        if (isPerfect) hapticEngine.perform(HapticType.PerfectRun)

        // Award gems in one shot — level-up bonus computed across the whole payout.
        val levelBefore = PlayerLevel.fromGems(totalGems)
        totalGems = settingsRepository.addGems(payout.total)
        var sessionGems = payout.total
        var levelUpGems = 0
        var levelUpTo: PlayerLevel? = null
        val levelAfter = PlayerLevel.fromGems(totalGems)
        if (levelAfter.ordinal > levelBefore.ordinal) {
            levelUpTo = levelAfter
            val bonus = PlayerLevel.levelUpBonus(levelAfter)
            if (bonus > 0) {
                totalGems = settingsRepository.addGems(bonus)
                sessionGems += bonus
                levelUpGems = bonus
            }
        }
        playerState.updateGems(totalGems)

        val tightest = tightestCorrectDeltaE ?: gameEngine.deltaECurve.first()
        colorMemoryRepository.savePersonalBest(score = score, tightestDeltaE = tightest)

        val breakdown = buildList {
            if (payout.correctGems > 0) add(GemAward("Correct rounds ×$correctCount", payout.correctGems))
            if (payout.streakBonusGems > 0) add(GemAward("Streak bonus", payout.streakBonusGems))
            if (payout.perfectBonusGems > 0) {
                add(GemAward("Perfect run bonus", payout.perfectBonusGems))
            }
            if (levelUpGems > 0) add(GemAward("Level up bonus", levelUpGems))
        }
        sessionResultCache.set(
            SessionResult(
                gameId = GameId.COLOR_MEMORY,
                deltaE = tightest,
                roundsSurvived = correctCount,
                correctRounds = correctCount,
                totalRounds = gameEngine.totalRounds,
                gemsEarned = sessionGems,
                gemBreakdown = breakdown,
                score = score,
                longestStreak = ColorMemoryRewards.longestStreak(roundResults),
                levelUpTo = levelUpTo,
            ),
        )
        _navEvent.emit(CMMatchNavEvent.NavigateToResult)
    }

    private companion object {
        const val MEMORY_MS = 3000L
        const val HOLD_MS = 400L
        const val FEEDBACK_MS = 1700L
    }
}
