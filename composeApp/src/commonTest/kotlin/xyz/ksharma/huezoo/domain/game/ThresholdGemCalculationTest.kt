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
import xyz.ksharma.huezoo.navigation.SessionResult
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for Threshold gem earn logic — all driven through [ThresholdViewModel].
 *
 * ## Milestone behaviour (actual code vs. original spec)
 * The [ThresholdViewModel] stores `awardedMilestones: MutableSet<Float>` and
 * clears it **on every wrong tap** (`awardedMilestones.clear()` in
 * `handleWrongTap`).  Milestones are therefore per-try, not per-session.
 * Tests reflect the actual code behaviour.
 *
 * ## ΔE maths (key tap counts)
 * Round n is shown at ΔE = 5.0 − (n−1)×0.3:
 *   - Tap 12 → ΔE = 1.7 < 2.0 → Sharp  (+5)
 *   - Tap 15 → ΔE = 0.8 < 1.0 → Expert (+10)
 *   - Tap 17 → ΔE = 0.2 < 0.5 → Elite  (+25)
 *
 * ## Setup
 * All dependencies are fake in-memory objects (no real DB, no Dispatchers.Default),
 * so [StandardTestDispatcher] controls ALL coroutine scheduling including the
 * repository calls inside [ThresholdViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThresholdGemCalculationTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeTest
    fun setup() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun teardown() = Dispatchers.resetMain()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildViewModel(
        thresholdRepo: FakeThresholdRepository = FakeThresholdRepository(),
    ): Pair<ThresholdViewModel, SessionResultCache> {
        val cache = SessionResultCache()
        val vm = ThresholdViewModel(
            gameEngine = FakeThresholdGameEngine(),
            repository = thresholdRepo,
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
     * Drives a complete Threshold session through [MAX_ATTEMPTS] tries.
     *
     * [correctTapsPerTry] specifies how many correct taps to make in each try
     * before the inevitable wrong tap that ends it. If the list is shorter than
     * [MAX_ATTEMPTS] the remaining tries use 0 correct taps.
     *
     * Returns the [SessionResult] placed in the cache at session end.
     *
     * Note: [FakeThresholdGameEngine] always places the odd swatch at index 0.
     * Index 0 = correct tap, index 1 = wrong tap.
     */
    private suspend fun TestScope.driveSession(
        vm: ThresholdViewModel,
        cache: SessionResultCache,
        vararg correctTapsPerTry: Int,
    ): SessionResult {
        advanceUntilIdle() // loadGame() + startSession() complete
        repeat(MAX_ATTEMPTS) { tryIdx ->
            val correctTaps = correctTapsPerTry.getOrElse(tryIdx) { 0 }
            repeat(correctTaps) {
                vm.onUiEvent(SwatchTapped(CORRECT))
                advanceUntilIdle()
            }
            vm.onUiEvent(SwatchTapped(WRONG))
            advanceUntilIdle()
        }
        return assertNotNull(cache.result.value, "SessionResult must be set after all tries exhausted")
    }

    // ── Base tap reward ───────────────────────────────────────────────────────

    @Test
    fun `first correct tap awards 2 gems`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 1) // 1 correct in try 1, rest wrong-only
        assertEquals(GameRewardRates.THRESHOLD_CORRECT_TAP, result.gemsEarned)
    }

    @Test
    fun `N correct taps in a try accumulate N times 2 gems`() = runTest(testDispatcher) {
        // 5 correct taps at ΔE 5.0, 4.7, 4.4, 4.1, 3.8 — all above 2.0, no milestone crossed
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 5)
        assertEquals(5 * GameRewardRates.THRESHOLD_CORRECT_TAP, result.gemsEarned)
    }

    @Test
    fun `wrong tap earns 0 gems for that tap`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache) // all tries: immediate wrong tap
        assertEquals(0, result.gemsEarned)
    }

    // ── Milestone: Sharp (ΔE < 2.0) ──────────────────────────────────────────

    @Test
    fun `crossing ΔE below 2_0 for the first time awards Sharp bonus of 5`() = runTest(testDispatcher) {
        // Tap 12 has ΔE = 5.0 - 11×0.3 = 1.7 < 2.0 → Sharp
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 12)
        val expected = 12 * GameRewardRates.THRESHOLD_CORRECT_TAP + GameRewardRates.THRESHOLD_MILESTONE_SHARP
        assertEquals(expected, result.gemsEarned)
    }

    @Test
    fun `Sharp milestone not awarded twice within the same try`() = runTest(testDispatcher) {
        // 13 taps: tap 12 crosses Sharp (ΔE 1.7 < 2.0), tap 13 is still below 2.0
        // but awardedMilestones already contains 2.0f → no second bonus
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 13)
        val expected = 13 * GameRewardRates.THRESHOLD_CORRECT_TAP + GameRewardRates.THRESHOLD_MILESTONE_SHARP
        assertEquals(expected, result.gemsEarned)
    }

    // ── Milestone: Expert (ΔE < 1.0) ─────────────────────────────────────────

    @Test
    fun `crossing ΔE below 1_0 for the first time awards Expert bonus of 10`() = runTest(testDispatcher) {
        // Tap 15: ΔE = 5.0 - 14×0.3 = 0.8 < 1.0 → Expert (+10); Sharp already at tap 12
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 15)
        val milestones = GameRewardRates.THRESHOLD_MILESTONE_SHARP + GameRewardRates.THRESHOLD_MILESTONE_EXPERT
        assertEquals(15 * GameRewardRates.THRESHOLD_CORRECT_TAP + milestones, result.gemsEarned)
    }

    @Test
    fun `Expert milestone not awarded twice within the same try`() = runTest(testDispatcher) {
        // Tap 15 → Expert (ΔE ≈ 0.8 < 1.0). After decrement, ΔE ≈ 0.4999 (IEEE 754 float
        // drift from repeated 0.3 subtractions) which is also < 1.0, but
        // awardedMilestones.add(1.0f) returns false → Expert not re-awarded.
        // That tap instead triggers Elite (< 0.5). If Expert were double-awarded the total
        // would be 16×2 + Sharp + Expert×2 + Elite = 16×2 + 5+20+25 = 82; actual = 72.
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 16)
        val allMilestones = GameRewardRates.THRESHOLD_MILESTONE_SHARP +
            GameRewardRates.THRESHOLD_MILESTONE_EXPERT +
            GameRewardRates.THRESHOLD_MILESTONE_ELITE
        assertEquals(16 * GameRewardRates.THRESHOLD_CORRECT_TAP + allMilestones, result.gemsEarned)
    }

    // ── Milestone: Elite (ΔE < 0.5) ──────────────────────────────────────────

    @Test
    fun `crossing ΔE below 0_5 awards Elite bonus of 25`() = runTest(testDispatcher) {
        // Tap 17: ΔE = 5.0 - 16×0.3 = 0.2 < 0.5 → Elite (+25); all three milestones earned
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 17)
        val allMilestones = GameRewardRates.THRESHOLD_MILESTONE_SHARP +
            GameRewardRates.THRESHOLD_MILESTONE_EXPERT +
            GameRewardRates.THRESHOLD_MILESTONE_ELITE
        assertEquals(17 * GameRewardRates.THRESHOLD_CORRECT_TAP + allMilestones, result.gemsEarned)
    }

    @Test
    fun `Elite milestone not awarded twice within the same try`() = runTest(testDispatcher) {
        // 18 taps: tap 17 → Elite (ΔE=0.2); tap 18 at ΔE=0.1 < 0.5
        // but awardedMilestones already contains 0.5f → no second Elite
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 18)
        val allMilestones = GameRewardRates.THRESHOLD_MILESTONE_SHARP +
            GameRewardRates.THRESHOLD_MILESTONE_EXPERT +
            GameRewardRates.THRESHOLD_MILESTONE_ELITE
        assertEquals(18 * GameRewardRates.THRESHOLD_CORRECT_TAP + allMilestones, result.gemsEarned)
    }

    // ── Cross-try milestone behaviour ─────────────────────────────────────────

    @Test
    fun `milestones reset per try so are re-earned in subsequent tries`() = runTest(testDispatcher) {
        // awardedMilestones.clear() is called on every wrong tap (per-try, not per-session).
        // Try 1: 12 correct → Sharp (+5) → wrong. Try 2: 12 correct → Sharp again (+5) → wrong.
        // Total milestone gems = 10 (5 per try × 2 tries).
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 12, 12) // Sharp earned in both try 1 and try 2
        val tapGems = (12 + 12) * GameRewardRates.THRESHOLD_CORRECT_TAP
        val milestoneGems = GameRewardRates.THRESHOLD_MILESTONE_SHARP * 2
        assertEquals(tapGems + milestoneGems, result.gemsEarned)
    }

    @Test
    fun `gems accumulate correctly across two tries`() = runTest(testDispatcher) {
        // Try 1: 3 correct (6 gems, no milestone) + wrong.
        // Try 2: 4 correct (8 gems, no milestone) + wrong.
        // Tries 3–5: wrong only.
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 3, 4)
        assertEquals((3 + 4) * GameRewardRates.THRESHOLD_CORRECT_TAP, result.gemsEarned)
    }

    // ── Streak-10 bonus ───────────────────────────────────────────────────────

    @Test
    fun `10 consecutive correct taps in one try awards streak bonus of 15 gems`() = runTest(testDispatcher) {
        // 10 taps reach ΔE = 5.0 - 9×0.3 = 2.3 — no milestone crossed yet.
        // Only gem sources: 10 tap rewards + THRESHOLD_STREAK_10_BONUS.
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 10)
        val expected = 10 * GameRewardRates.THRESHOLD_CORRECT_TAP + GameRewardRates.THRESHOLD_STREAK_10_BONUS
        assertEquals(expected, result.gemsEarned)
    }

    @Test
    fun `streak bonus not awarded for 9 consecutive taps`() = runTest(testDispatcher) {
        // 9 taps: streak is 9, never hits 10 → no bonus
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 9)
        assertEquals(9 * GameRewardRates.THRESHOLD_CORRECT_TAP, result.gemsEarned)
    }

    @Test
    fun `wrong tap resets streak so partial streak in try 1 does not carry into try 2`() = runTest(testDispatcher) {
        // Try 1: 9 correct → wrong (consecutiveCorrect resets to 0)
        // Try 2: 9 correct → wrong (streak reaches 9 again, not 18 — no bonus)
        // No streak bonus expected in either try.
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 9, 9)
        val expected = (9 + 9) * GameRewardRates.THRESHOLD_CORRECT_TAP
        assertEquals(expected, result.gemsEarned)
    }

    @Test
    fun `streak bonus can be re-earned in a fresh try after a wrong tap`() = runTest(testDispatcher) {
        // Try 1: 9 correct → wrong (no streak bonus; consecutiveCorrect resets)
        // Try 2: 10 correct → streak bonus awarded; then wrong tap ends try
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 9, 10)
        val tapGems = (9 + 10) * GameRewardRates.THRESHOLD_CORRECT_TAP
        assertEquals(tapGems + GameRewardRates.THRESHOLD_STREAK_10_BONUS, result.gemsEarned)
    }

    @Test
    fun `streak bonus appears in gem breakdown under milestone bonuses`() = runTest(testDispatcher) {
        // 10 taps → streak bonus goes into sessionMilestoneGems → "Milestone bonuses" line
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 10)
        val milestoneLine = result.gemBreakdown.find { it.label == "Milestone bonuses" }
        assertNotNull(milestoneLine, "Milestone bonuses line must be present when streak bonus earned")
        assertEquals(GameRewardRates.THRESHOLD_STREAK_10_BONUS, milestoneLine.amount)
    }

    // ── GemBreakdown structure ────────────────────────────────────────────────

    @Test
    fun `gem breakdown has correct tap line when taps earned`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 5)
        val tapLine = result.gemBreakdown.find { it.label == "Correct taps" }
        assertNotNull(tapLine)
        assertEquals(5 * GameRewardRates.THRESHOLD_CORRECT_TAP, tapLine.amount)
    }

    @Test
    fun `gem breakdown has milestone line when milestone earned`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 12) // Sharp milestone crossed
        val milestoneLine = result.gemBreakdown.find { it.label == "Milestone bonuses" }
        assertNotNull(milestoneLine)
        assertEquals(GameRewardRates.THRESHOLD_MILESTONE_SHARP, milestoneLine.amount)
    }

    @Test
    fun `gem breakdown omits milestone line when no milestone earned`() = runTest(testDispatcher) {
        val (vm, cache) = buildViewModel()
        val result = driveSession(vm, cache, 3) // 3 taps: ΔE 5.0, 4.7, 4.4 — all above 2.0
        val milestoneLine = result.gemBreakdown.find { it.label == "Milestone bonuses" }
        assertNull(milestoneLine, "No milestone line expected when no milestone earned")
    }

    private companion object {
        const val CORRECT = 0 // odd swatch index in FakeThresholdGameEngine
        const val WRONG = 1
    }
}
