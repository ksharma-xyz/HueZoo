package xyz.ksharma.huezoo.domain.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.huezoo.navigation.SessionResult
import xyz.ksharma.huezoo.domain.color.FakeColorEngine
import xyz.ksharma.huezoo.testutil.FakeDailyGameEngine
import xyz.ksharma.huezoo.testutil.FakeDailyRepository
import xyz.ksharma.huezoo.testutil.FakeHapticEngine
import xyz.ksharma.huezoo.testutil.FakeSettingsRepository
import xyz.ksharma.huezoo.ui.games.daily.DailyViewModel
import xyz.ksharma.huezoo.ui.games.daily.state.DailyUiEvent.SwatchTapped
import xyz.ksharma.huezoo.ui.model.PlayerState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Daily Challenge gem earn logic — all driven through [DailyViewModel].
 *
 * The game always plays exactly [FakeDailyGameEngine.totalRounds] = 6 rounds.
 * ΔE curve: [4.0, 3.0, 2.0, 1.5, 1.0, 0.7] (easy → hard).
 *
 * Gem formula:
 * ```
 * gemsEarned = DAILY_PARTICIPATION                              // always +3
 *            + correctRounds × DAILY_CORRECT_ROUND             // +5 each
 *            + (if perfectRun) DAILY_PERFECT_BONUS             // +20
 * ```
 *
 * | Scenario    | correct | gems            |
 * |-------------|---------|-----------------|
 * | 0/6         |    0    | 3               |
 * | 1/6         |    1    | 3 + 5 = 8       |
 * | 3/6         |    3    | 3 + 15 = 18     |
 * | 5/6         |    5    | 3 + 25 = 28     |
 * | 6/6 perfect |    6    | 3 + 30 + 20 = 53|
 *
 * [FakeDailyGameEngine] always places the odd swatch at index 0 so tests
 * control correctness with index 0 (correct) or index 1 (wrong).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyGemCalculationTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeTest
    fun setup() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun teardown() = Dispatchers.resetMain()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildViewModel(): Pair<DailyViewModel, SessionResultCache> {
        val cache = SessionResultCache()
        val vm = DailyViewModel(
            gameEngine = FakeDailyGameEngine(),
            repository = FakeDailyRepository(),
            settingsRepository = FakeSettingsRepository(),
            colorEngine = FakeColorEngine(),
            playerState = PlayerState(),
            hapticEngine = FakeHapticEngine(),
            sessionResultCache = cache,
        )
        return vm to cache
    }

    /**
     * Drives a complete Daily session by playing all 6 rounds.
     *
     * [correctPerRound] specifies whether each round (0-indexed) is answered
     * correctly. Defaults to wrong (false) for rounds not listed.
     *
     * Index 0 = odd swatch (correct), index 1 = non-odd swatch (wrong).
     */
    private suspend fun TestScope.driveSession(
        vm: DailyViewModel,
        cache: SessionResultCache,
        vararg correctPerRound: Boolean,
    ): SessionResult {
        advanceUntilIdle() // loadGame() + emitRound() complete
        repeat(6) { roundIdx ->
            val isCorrect = correctPerRound.getOrElse(roundIdx) { false }
            vm.onUiEvent(SwatchTapped(if (isCorrect) CORRECT else WRONG))
            advanceUntilIdle()
        }
        return assertNotNull(cache.result.value, "SessionResult must be set after 6 rounds")
    }

    // ── Participation gem ─────────────────────────────────────────────────────

    @Test
    fun `finishing all 6 rounds always awards 3 participation gems`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache) // all wrong
        assertEquals(GameRewardRates.DAILY_PARTICIPATION, result.gemsEarned)
    }

    // ── Per-round correct reward ──────────────────────────────────────────────

    @Test
    fun `1 correct round out of 6 awards 8 gems`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, true) // round 1 correct, 2–6 wrong
        assertEquals(GameRewardRates.DAILY_PARTICIPATION + GameRewardRates.DAILY_CORRECT_ROUND, result.gemsEarned)
    }

    @Test
    fun `3 correct rounds out of 6 awards 18 gems`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, true, false, true, false, true, false)
        val expected = GameRewardRates.DAILY_PARTICIPATION + 3 * GameRewardRates.DAILY_CORRECT_ROUND
        assertEquals(expected, result.gemsEarned)
    }

    @Test
    fun `5 correct rounds out of 6 awards 28 gems`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, true, true, true, true, true, false) // round 6 wrong
        val expected = GameRewardRates.DAILY_PARTICIPATION + 5 * GameRewardRates.DAILY_CORRECT_ROUND
        assertEquals(expected, result.gemsEarned)
    }

    // ── Perfect bonus ─────────────────────────────────────────────────────────

    @Test
    fun `6 out of 6 correct awards perfect bonus giving 53 gems total`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, true, true, true, true, true, true)
        val expected = GameRewardRates.DAILY_PARTICIPATION +
            6 * GameRewardRates.DAILY_CORRECT_ROUND +
            GameRewardRates.DAILY_PERFECT_BONUS
        assertEquals(expected, result.gemsEarned)
    }

    @Test
    fun `perfect bonus is NOT awarded when any round is wrong`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, true, true, true, true, true, false) // 5/6
        val withoutPerfect = GameRewardRates.DAILY_PARTICIPATION + 5 * GameRewardRates.DAILY_CORRECT_ROUND
        val withPerfect = withoutPerfect + GameRewardRates.DAILY_PERFECT_BONUS
        assertEquals(withoutPerfect, result.gemsEarned)
        assertTrue(result.gemsEarned < withPerfect)
    }

    // ── GemBreakdown structure ────────────────────────────────────────────────

    @Test
    fun `gem breakdown always includes participation line`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache) // all wrong
        val participationLine = result.gemBreakdown.find { it.label == "Participation" }
        assertNotNull(participationLine)
        assertEquals(GameRewardRates.DAILY_PARTICIPATION, participationLine.amount)
    }

    @Test
    fun `gem breakdown includes correct rounds line when any are correct`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, true, false, true, false, true, false) // 3 correct
        val correctLine = result.gemBreakdown.find { it.label.startsWith("Correct rounds") }
        assertNotNull(correctLine, "Expected 'Correct rounds ×3' line in breakdown")
        assertEquals(3 * GameRewardRates.DAILY_CORRECT_ROUND, correctLine.amount)
    }

    @Test
    fun `gem breakdown includes perfect bonus line only on 6 out of 6`() = runTest(testDispatcher) {
        val (vm6, cache6) = buildViewModel()
        val perfect = driveSession(vm6, cache6, true, true, true, true, true, true)
        assertTrue(perfect.gemBreakdown.any { it.label == "Perfect run bonus" })

        val (vm5, cache5) = buildViewModel()
        val imperfect = driveSession(vm5, cache5, true, true, true, true, true, false)
        assertTrue(imperfect.gemBreakdown.none { it.label == "Perfect run bonus" })
    }

    @Test
    fun `gem breakdown omits correct rounds line when 0 correct`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache) // all wrong
        val correctLine = result.gemBreakdown.find { it.label.startsWith("Correct rounds") }
        assertNull(correctLine, "No correct-rounds line expected when 0 correct")
    }

    // ── SessionResult fields ──────────────────────────────────────────────────

    @Test
    fun `SessionResult correctRounds reflects actual correct tap count`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        // 4 correct rounds → correctRounds=4, totalRounds=6
        val result = driveSession(vm, cache, true, true, true, true, false, false)
        assertEquals(4, result.correctRounds)
        assertEquals(6, result.totalRounds)
    }

    @Test
    fun `SessionResult deltaE reflects highest ΔE among correctly answered rounds`() = runTest(testDispatcher) {
        // ΔE curve: [4.0, 3.0, 2.0, 1.5, 1.0, 0.7]
        // Rounds 1–3 correct (ΔE 4.0, 3.0, 2.0), round 4 wrong.
        // highestCorrectDeltaE = max(4.0, 3.0, 2.0) = 4.0
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, true, true, true, false, false, false)
        assertEquals(4.0f, result.deltaE)
    }

    private companion object {
        const val CORRECT = 0 // odd swatch index in FakeDailyGameEngine
        const val WRONG = 1
    }
}
