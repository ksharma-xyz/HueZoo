package xyz.ksharma.huezoo.domain.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.huezoo.data.repository.impl.DefaultSettingsRepository
import xyz.ksharma.huezoo.data.repository.impl.DefaultThresholdRepository
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.SessionResult
import xyz.ksharma.huezoo.domain.color.FakeColorEngine
import xyz.ksharma.huezoo.testutil.FakeDailyRepository
import xyz.ksharma.huezoo.testutil.FakeHapticEngine
import xyz.ksharma.huezoo.testutil.FakePlatformOps
import xyz.ksharma.huezoo.testutil.FakeSettingsRepository
import xyz.ksharma.huezoo.testutil.FakeThresholdGameEngine
import xyz.ksharma.huezoo.testutil.FakeThresholdRepository
import xyz.ksharma.huezoo.testutil.createTestDatabase
import xyz.ksharma.huezoo.ui.games.threshold.ThresholdViewModel
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiEvent.SwatchTapped
import xyz.ksharma.huezoo.ui.games.threshold.state.ThresholdUiState
import xyz.ksharma.huezoo.ui.model.PlayerState
import xyz.ksharma.huezoo.ui.result.ResultViewModel
import xyz.ksharma.huezoo.ui.result.state.ResultUiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Threshold ΔE progression and personal best tracking.
 *
 * ## Coverage
 * - **ΔE progression / bestDeltaE** — driven through [ThresholdViewModel] with fake deps.
 * - **isNewPersonalBest** — driven through [ResultViewModel] with a fake repo.
 * - **Personal best persistence** — implemented using real [DefaultThresholdRepository]
 *   backed by an in-memory SQLite database.
 *
 * [FakeThresholdGameEngine] always places the odd swatch at index 0:
 * - `SwatchTapped(0)` = correct tap
 * - `SwatchTapped(1)` = wrong tap
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThresholdDeltaEProgressionTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeTest
    fun setupDispatcher() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun teardownDispatcher() = Dispatchers.resetMain()

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
        )
        return vm to cache
    }

    private fun fakeThresholdResult(deltaE: Float) = SessionResult(
        gameId = GameId.THRESHOLD,
        deltaE = deltaE,
        roundsSurvived = 3,
        correctRounds = 3,
        totalRounds = 5,
        gemsEarned = 6,
        gemBreakdown = emptyList(),
    )

    // ─── ΔE progression (ThresholdViewModel) ──────────────────────────────────

    @Test
    fun `first round starts at ΔE 5_0`() = runTest(testDispatcher) {
        val (vm, _) = buildViewModel()
        advanceUntilIdle() // loadGame() + startSession() + emitRound()
        val state = assertIs<ThresholdUiState.Playing>(vm.uiState.value)
        assertEquals(ThresholdGameEngine.STARTING_DELTA_E, state.deltaE)
    }

    @Test
    fun `correct tap decreases ΔE by 0_3`() = runTest(testDispatcher) {
        val (vm, _) = buildViewModel()
        advanceUntilIdle()
        vm.onUiEvent(SwatchTapped(0)) // correct
        advanceUntilIdle()
        val state = assertIs<ThresholdUiState.Playing>(vm.uiState.value)
        assertEquals(ThresholdGameEngine.STARTING_DELTA_E - ThresholdGameEngine.DELTA_E_STEP, state.deltaE)
    }

    @Test
    fun `wrong tap resets ΔE to 5_0 for the next try`() = runTest(testDispatcher) {
        val (vm, _) = buildViewModel()
        advanceUntilIdle()
        repeat(3) { vm.onUiEvent(SwatchTapped(0)); advanceUntilIdle() } // 3 correct → ΔE 4.1
        vm.onUiEvent(SwatchTapped(1)); advanceUntilIdle() // wrong → new try
        val state = assertIs<ThresholdUiState.Playing>(vm.uiState.value)
        assertEquals(ThresholdGameEngine.STARTING_DELTA_E, state.deltaE)
    }

    @Test
    fun `ΔE is coerced to minimum 0_1`() = runTest(testDispatcher) {
        val (vm, _) = buildViewModel()
        advanceUntilIdle()
        // 18 correct taps: tap 17 → ΔE=0.2 → after decrement 0.1; tap 18 should emit at 0.1
        repeat(18) { vm.onUiEvent(SwatchTapped(0)); advanceUntilIdle() }
        val state = assertIs<ThresholdUiState.Playing>(vm.uiState.value)
        assertEquals(ThresholdGameEngine.MIN_DELTA_E, state.deltaE)
    }

    // ─── bestDeltaE within a session (ThresholdViewModel) ─────────────────────

    @Test
    fun `bestDeltaE tracks lowest ΔE reached before each wrong tap`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        advanceUntilIdle()
        // Try 1: 3 correct → tap ΔE values: 5.0, ~4.7, ~4.4 → bestDeltaE ≈ 4.4 → wrong
        repeat(3) { vm.onUiEvent(SwatchTapped(0)); advanceUntilIdle() }
        vm.onUiEvent(SwatchTapped(1)); advanceUntilIdle()
        // Try 2: 2 correct → ΔE ~5.0, ~4.7 — both above ~4.4, bestDeltaE unchanged → wrong
        repeat(2) { vm.onUiEvent(SwatchTapped(0)); advanceUntilIdle() }
        vm.onUiEvent(SwatchTapped(1)); advanceUntilIdle()
        repeat(ThresholdGameEngine.MAX_ATTEMPTS - 2) {
            vm.onUiEvent(SwatchTapped(1)); advanceUntilIdle()
        }
        // bestDeltaE ≈ 5.0 - 2×0.3 = 4.4 (within float tolerance)
        val deltaE = cache.result.value!!.deltaE
        assertTrue(deltaE < 4.6f, "bestDeltaE should be below try-2 minimum (~4.7) → got $deltaE")
        assertTrue(deltaE > 4.2f, "bestDeltaE should not have gone below 4.4 range → got $deltaE")
    }

    @Test
    fun `bestDeltaE does not regress when later try achieves higher ΔE`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        advanceUntilIdle()
        // Try 1: 12 correct → bestDeltaE ≈ 5.0 - 11×0.3 ≈ 1.7 (tap 12); wrong
        repeat(12) { vm.onUiEvent(SwatchTapped(0)); advanceUntilIdle() }
        vm.onUiEvent(SwatchTapped(1)); advanceUntilIdle()
        // Try 2: 2 correct → ΔE ~5.0, ~4.7 — both above ~1.7; bestDeltaE unchanged; wrong
        repeat(2) { vm.onUiEvent(SwatchTapped(0)); advanceUntilIdle() }
        vm.onUiEvent(SwatchTapped(1)); advanceUntilIdle()
        repeat(ThresholdGameEngine.MAX_ATTEMPTS - 2) {
            vm.onUiEvent(SwatchTapped(1)); advanceUntilIdle()
        }
        // bestDeltaE ≈ 1.7 (within float tolerance); must be well below try-2 min (~4.7)
        val deltaE = cache.result.value!!.deltaE
        assertTrue(deltaE < 2.0f, "bestDeltaE should stay near 1.7, not regress to try-2 ΔE → got $deltaE")
        assertTrue(deltaE > 1.5f, "bestDeltaE should be in the ~1.7 range → got $deltaE")
    }

    // ─── Personal best persistence (DefaultThresholdRepository) ───────────────
    //
    // Each test gets a fresh in-memory database — no shared state between tests.

    private val db = createTestDatabase()
    private val repo = DefaultThresholdRepository(
        db = db,
        platformOps = FakePlatformOps(),
        settingsRepository = DefaultSettingsRepository(db),
    )

    @Test
    fun `savePersonalBest stores ΔE when no previous best exists`() = runBlocking {
        assertNull(repo.getPersonalBest(), "DB should be empty before any save")
        repo.savePersonalBest(2.5f)
        assertEquals(2.5f, repo.getPersonalBest()?.bestDeltaE)
    }

    @Test
    fun `savePersonalBest updates stored best when new ΔE is strictly lower`() = runBlocking {
        repo.savePersonalBest(3.0f)
        repo.savePersonalBest(2.5f)
        assertEquals(2.5f, repo.getPersonalBest()?.bestDeltaE)
    }

    @Test
    fun `savePersonalBest does NOT update when new ΔE is equal to stored best`() = runBlocking {
        repo.savePersonalBest(2.5f)
        repo.savePersonalBest(2.5f) // strictly-less check: equal must not overwrite
        assertEquals(2.5f, repo.getPersonalBest()?.bestDeltaE)
    }

    @Test
    fun `savePersonalBest does NOT update when new ΔE is higher than stored best`() = runBlocking {
        repo.savePersonalBest(2.0f)
        repo.savePersonalBest(3.0f) // higher ΔE = worse; must not overwrite
        assertEquals(2.0f, repo.getPersonalBest()?.bestDeltaE)
    }

    // ─── isNewPersonalBest in ResultViewModel ─────────────────────────────────

    @Test
    fun `isNewPersonalBest is true when session ΔE equals stored best within tolerance`() =
        runTest(testDispatcher) {
            val storedBest = 2.5f
            val cache = SessionResultCache()
            val vm = ResultViewModel(
                sessionResultCache = cache,
                thresholdRepository = FakeThresholdRepository(initialBestDeltaE = storedBest),
                dailyRepository = FakeDailyRepository(),
            )
            advanceUntilIdle()
            cache.set(fakeThresholdResult(deltaE = storedBest)) // abs(2.5 - 2.5) = 0 < 0.005
            advanceUntilIdle()
            assertTrue(assertIs<ResultUiState.Ready>(vm.uiState.value).isNewPersonalBest)
        }

    @Test
    fun `isNewPersonalBest is false when session ΔE is worse than stored best`() =
        runTest(testDispatcher) {
            val cache = SessionResultCache()
            val vm = ResultViewModel(
                sessionResultCache = cache,
                thresholdRepository = FakeThresholdRepository(initialBestDeltaE = 2.0f),
                dailyRepository = FakeDailyRepository(),
            )
            advanceUntilIdle()
            cache.set(fakeThresholdResult(deltaE = 3.0f)) // 3.0 > 2.0 → worse
            advanceUntilIdle()
            assertFalse(assertIs<ResultUiState.Ready>(vm.uiState.value).isNewPersonalBest)
        }

    @Test
    fun `isNewPersonalBest is true when no previous personal best exists`() =
        runTest(testDispatcher) {
            val cache = SessionResultCache()
            val vm = ResultViewModel(
                sessionResultCache = cache,
                thresholdRepository = FakeThresholdRepository(), // no stored best
                dailyRepository = FakeDailyRepository(),
            )
            advanceUntilIdle()
            cache.set(fakeThresholdResult(deltaE = 2.5f))
            advanceUntilIdle()
            assertTrue(assertIs<ResultUiState.Ready>(vm.uiState.value).isNewPersonalBest)
        }
}
