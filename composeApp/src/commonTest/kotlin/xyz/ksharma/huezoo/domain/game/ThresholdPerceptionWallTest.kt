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
import xyz.ksharma.huezoo.domain.color.FakeColorEngine
import xyz.ksharma.huezoo.domain.game.ThresholdGameEngine.Companion.MAX_ATTEMPTS
import xyz.ksharma.huezoo.platform.ads.AdOrchestrator
import xyz.ksharma.huezoo.testutil.FakeHapticEngine
import xyz.ksharma.huezoo.testutil.FakePlatformOps
import xyz.ksharma.huezoo.testutil.FakeSettingsRepository
import xyz.ksharma.huezoo.testutil.FakeThresholdGameEngine
import xyz.ksharma.huezoo.testutil.FakeThresholdRepository
import xyz.ksharma.huezoo.ui.games.threshold.ThresholdViewModel
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiEvent.SwatchTapped
import xyz.ksharma.huezoo.ui.model.PlayerState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for perception wall behaviour — all driven through [ThresholdViewModel].
 *
 * ## The Perception Wall
 * [ThresholdGameEngine.MIN_DELTA_E] = 0.1 is the ΔE floor. After 17 correct taps,
 * [currentDeltaE] clamps to MIN_DELTA_E. The 18th correct tap ([WALL_TAPS]) triggers
 * the perception wall: the current life ends triumphantly, the 5 000-gem bonus is
 * awarded (at most once per session), and the next life starts at STARTING_DELTA_E.
 *
 * ## Key invariants under test
 * - Life ALWAYS ends when the player correctly identifies at MIN_DELTA_E — even if
 *   [hitPerceptionWall] is already `true` (the bug this test suite guards against).
 * - The 5 000-gem bonus is awarded at most once per session regardless of how many
 *   times the player reaches MIN_DELTA_E.
 * - [SessionResult.hitPerceptionWall] is `true` after any wall hit.
 *
 * ## Tap count to reach MIN_DELTA_E
 * Round n is shown at ΔE = 5.0 − (n−1)×0.3. After tap 17 the ΔE update clamps to
 * 0.1 f, so the 18th correct tap ([WALL_TAPS] = 18) is presented at 0.1 and triggers
 * the wall.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThresholdPerceptionWallTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeTest
    fun setup() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun teardown() = Dispatchers.resetMain()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildViewModel(): Pair<ThresholdViewModel, SessionResultCache> {
        val cache = SessionResultCache()
        val vm = ThresholdViewModel(
            gameEngine = FakeThresholdGameEngine(),
            repository = FakeThresholdRepository(),
            colorEngine = FakeColorEngine(),
            settingsRepository = FakeSettingsRepository(),
            playerState = PlayerState(),
            hapticEngine = FakeHapticEngine(),
            sessionResultCache = cache,
            adOrchestrator = AdOrchestrator(),
            platformOps = FakePlatformOps(),
        )
        return vm to cache
    }

    /**
     * Sends [WALL_TAPS] correct taps, triggering the perception wall.
     * [ThresholdViewModel.endLifeAfterWall] starts the next try automatically — no wrong
     * tap is needed; the ViewModel transitions the life internally.
     */
    private suspend fun TestScope.driveToWall(vm: ThresholdViewModel) {
        repeat(WALL_TAPS) {
            vm.onUiEvent(SwatchTapped(CORRECT))
            advanceUntilIdle()
        }
    }

    // ── Core wall behaviour ───────────────────────────────────────────────────

    @Test
    fun `session ends normally after one wall hit and exhausting remaining tries`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        advanceUntilIdle() // loadGame + startSession

        // Try 1: 18 correct → wall hit → endLifeAfterWall starts try 2
        driveToWall(vm)

        // Exhaust remaining 4 tries with wrong taps (tries 2–5)
        repeat(MAX_ATTEMPTS - 1) {
            vm.onUiEvent(SwatchTapped(WRONG))
            advanceUntilIdle()
        }

        val result = assertNotNull(cache.result.value, "SessionResult must be set after all tries exhausted")
        assertTrue(result.hitPerceptionWall, "hitPerceptionWall must be true after wall hit")
    }

    @Test
    fun `game does not get stuck when hitting wall a second time with hitPerceptionWall already true`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        advanceUntilIdle()

        // Try 1: wall hit (bonus awarded, hitPerceptionWall = true, life ends)
        driveToWall(vm)
        // Try 2: wall hit again — before the fix the game was stuck at ΔE 0.1 forever
        // because the `!hitPerceptionWall` guard also blocked `endLifeAfterWall()`.
        driveToWall(vm)

        // Exhaust remaining 3 tries (tries 3–5)
        repeat(MAX_ATTEMPTS - 2) {
            vm.onUiEvent(SwatchTapped(WRONG))
            advanceUntilIdle()
        }

        val result = assertNotNull(
            cache.result.value,
            "Game must not get stuck — SessionResult must be set after all tries",
        )
        assertTrue(result.hitPerceptionWall)
    }

    @Test
    fun `perception wall bonus counted only once in gemsEarned across two wall hits`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        advanceUntilIdle()

        driveToWall(vm)  // try 1 → bonus awarded (5 000 gems)
        driveToWall(vm)  // try 2 → no bonus (hitPerceptionWall already true)
        // Exhaust tries 3–5
        repeat(MAX_ATTEMPTS - 2) {
            vm.onUiEvent(SwatchTapped(WRONG))
            advanceUntilIdle()
        }

        val result = assertNotNull(cache.result.value)
        assertTrue(result.hitPerceptionWall)

        // If bonus were awarded twice gemsEarned ≈ 10 000 + tap/milestone gems.
        // Tap/milestone gems from 36 taps (2 × 18) are well under 1 000, so the
        // upper bound of 2 × THRESHOLD_PERCEPTION_WALL (= 10 000) correctly
        // distinguishes "bonus once" (≈ 5 167) from "bonus twice" (≈ 10 167).
        assertTrue(
            result.gemsEarned < 2 * GameRewardRates.THRESHOLD_PERCEPTION_WALL,
            "Perception wall bonus awarded at most once — expected < " +
                "${2 * GameRewardRates.THRESHOLD_PERCEPTION_WALL}, got ${result.gemsEarned}",
        )
        assertTrue(
            result.gemsEarned >= GameRewardRates.THRESHOLD_PERCEPTION_WALL,
            "Perception wall bonus must be included — expected >= " +
                "${GameRewardRates.THRESHOLD_PERCEPTION_WALL}, got ${result.gemsEarned}",
        )
    }

    @Test
    fun `hitPerceptionWall is true in result when wall hit occurs on last try`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        advanceUntilIdle()

        // Exhaust tries 1–4 with wrong taps, then hit wall on try 5
        repeat(MAX_ATTEMPTS - 1) {
            vm.onUiEvent(SwatchTapped(WRONG))
            advanceUntilIdle()
        }
        // Try 5: drive to wall — endLifeAfterWall detects triesRemaining == 0 and navigates
        driveToWall(vm)

        val result = assertNotNull(cache.result.value, "Session must end when last try hits the wall")
        assertTrue(result.hitPerceptionWall, "hitPerceptionWall must be true when wall hit on last try")
    }

    private companion object {
        /**
         * Correct taps required to reach [ThresholdGameEngine.MIN_DELTA_E] (0.1 f).
         * Round n is at ΔE = 5.0 − (n−1)×0.3; after 17 correct taps currentDeltaE
         * clamps to 0.1, so the 18th tap is the wall trigger.
         */
        const val WALL_TAPS = 18
        const val CORRECT = 0 // FakeThresholdGameEngine always places odd swatch at index 0
        const val WRONG = 1
    }
}
