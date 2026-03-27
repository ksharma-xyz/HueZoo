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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.huezoo.data.repository.impl.DefaultDailyRepository
import xyz.ksharma.huezoo.navigation.GameId
import xyz.ksharma.huezoo.navigation.SessionResult
import xyz.ksharma.huezoo.testutil.FakeDailyRepository
import xyz.ksharma.huezoo.testutil.FakeSettingsRepository
import xyz.ksharma.huezoo.testutil.FakeThresholdRepository
import xyz.ksharma.huezoo.testutil.createTestDatabase
import xyz.ksharma.huezoo.ui.home.HomeViewModel
import xyz.ksharma.huezoo.ui.home.state.HomeUiState
import xyz.ksharma.huezoo.ui.result.ResultViewModel
import xyz.ksharma.huezoo.ui.result.state.ResultUiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Tests for Daily Challenge streak counting and personal best tracking.
 *
 * Uses a real [DefaultDailyRepository] with an in-memory SQLite database.
 * Each test class instance gets a fresh database (kotlin.test creates a new
 * instance per test method), so there is no shared state between tests.
 *
 * ViewModel tests (isNewPersonalBest, HomeViewModel streak) use fake in-memory
 * repositories so all coroutine scheduling stays on the test dispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class DailyStreakAndPersonalBestTest {

    private val repo = DefaultDailyRepository(createTestDatabase())

    private val TODAY = LocalDate(2026, 3, 27)
    private val YESTERDAY = LocalDate(2026, 3, 26)

    // ── Test dispatcher for ViewModel tests ───────────────────────────────────

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeTest
    fun setupDispatcher() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun teardownDispatcher() = Dispatchers.resetMain()

    // ─── Streak: basic cases ──────────────────────────────────────────────────

    @Test
    fun `streak is 0 when today is not completed`() = runBlocking {
        repo.saveCompletion(YESTERDAY) // yesterday completed, today is not
        assertEquals(0, repo.getStreak(TODAY))
    }

    @Test
    fun `streak is 1 when only today is completed`() = runBlocking {
        repo.saveCompletion(TODAY)
        assertEquals(1, repo.getStreak(TODAY))
    }

    @Test
    fun `streak is N for N consecutive completed days ending today`() = runBlocking {
        // Complete today and three days before — 4 consecutive days
        for (i in 0..3) {
            repo.saveCompletion(LocalDate(2026, 3, 27 - i))
        }
        assertEquals(4, repo.getStreak(TODAY))
    }

    @Test
    fun `streak breaks at the first missing day`() = runBlocking {
        // Complete today, yesterday, but skip day-2 — streak should be 2
        repo.saveCompletion(TODAY)
        repo.saveCompletion(YESTERDAY)
        repo.saveCompletion(LocalDate(2026, 3, 24)) // day-3 completed, but day-2 missing
        assertEquals(2, repo.getStreak(TODAY))
    }

    @Test
    fun `streak counts backward from today only`() = runBlocking {
        // Three consecutive completions but none is today
        repo.saveCompletion(LocalDate(2026, 3, 22))
        repo.saveCompletion(LocalDate(2026, 3, 23))
        repo.saveCompletion(LocalDate(2026, 3, 24))
        assertEquals(0, repo.getStreak(TODAY)) // today absent → streak 0
    }

    @Test
    fun `empty completion history gives streak 0`() = runBlocking {
        assertEquals(0, repo.getStreak(TODAY))
    }

    // ─── One-play-per-day gate ────────────────────────────────────────────────

    @Test
    fun `getChallenge returns completed challenge when today is marked completed`() = runBlocking {
        repo.saveCompletion(TODAY)
        val challenge = repo.getChallenge(TODAY)
        assertNotNull(challenge)
        assertTrue(challenge.completed)
    }

    @Test
    fun `getChallenge allows play on a new day even if yesterday is completed`() = runBlocking {
        repo.saveCompletion(YESTERDAY) // yesterday done
        val challenge = repo.getChallenge(TODAY) // today untouched
        // null means no record for today → player can start
        assertNull(challenge, "No record for today → player is free to play")
    }

    // ─── Personal best: rounds ────────────────────────────────────────────────

    @Test
    fun `savePersonalBest stores rounds when no previous best exists`() = runBlocking {
        assertNull(repo.getPersonalBest(), "DB should be empty before any save")
        repo.savePersonalBest(deltaE = 1.5f, roundsSurvived = 4)
        assertEquals(4, repo.getPersonalBest()?.bestRounds)
    }

    @Test
    fun `savePersonalBest updates when new rounds strictly exceed stored best`() = runBlocking {
        repo.savePersonalBest(deltaE = 2.0f, roundsSurvived = 3)
        repo.savePersonalBest(deltaE = 1.0f, roundsSurvived = 5)
        assertEquals(5, repo.getPersonalBest()?.bestRounds)
    }

    @Test
    fun `savePersonalBest does NOT update when new rounds equal stored best`() = runBlocking {
        repo.savePersonalBest(deltaE = 1.5f, roundsSurvived = 4)
        repo.savePersonalBest(deltaE = 0.7f, roundsSurvived = 4) // equal — must not overwrite
        assertEquals(4, repo.getPersonalBest()?.bestRounds)
    }

    @Test
    fun `savePersonalBest does NOT update when new rounds are fewer than stored best`() = runBlocking {
        repo.savePersonalBest(deltaE = 1.0f, roundsSurvived = 5)
        repo.savePersonalBest(deltaE = 0.7f, roundsSurvived = 3) // regression — must not overwrite
        assertEquals(5, repo.getPersonalBest()?.bestRounds)
    }

    // ─── isNewPersonalBest for daily in ResultViewModel ───────────────────────

    @Test
    fun `isNewPersonalBest true when correctRounds exceed stored bestRounds`() =
        runTest(testDispatcher) {
            val cache = SessionResultCache()
            val vm = ResultViewModel(
                sessionResultCache = cache,
                thresholdRepository = FakeThresholdRepository(),
                dailyRepository = FakeDailyRepository(initialBestRounds = 3),
            )
            advanceUntilIdle()
            cache.set(fakeDailyResult(roundsSurvived = 5)) // 5 > 3 → new best
            advanceUntilIdle()
            assertTrue(assertIs<ResultUiState.Ready>(vm.uiState.value).isNewPersonalBest)
        }

    @Test
    fun `isNewPersonalBest true when no previous personal best exists`() =
        runTest(testDispatcher) {
            val cache = SessionResultCache()
            val vm = ResultViewModel(
                sessionResultCache = cache,
                thresholdRepository = FakeThresholdRepository(),
                dailyRepository = FakeDailyRepository(), // no stored best
            )
            advanceUntilIdle()
            cache.set(fakeDailyResult(roundsSurvived = 4))
            advanceUntilIdle()
            assertTrue(assertIs<ResultUiState.Ready>(vm.uiState.value).isNewPersonalBest)
        }

    @Test
    fun `isNewPersonalBest false when correctRounds do not exceed stored bestRounds`() =
        runTest(testDispatcher) {
            val cache = SessionResultCache()
            val vm = ResultViewModel(
                sessionResultCache = cache,
                thresholdRepository = FakeThresholdRepository(),
                dailyRepository = FakeDailyRepository(initialBestRounds = 5),
            )
            advanceUntilIdle()
            cache.set(fakeDailyResult(roundsSurvived = 3)) // 3 < 5 → not new best
            advanceUntilIdle()
            assertFalse(assertIs<ResultUiState.Ready>(vm.uiState.value).isNewPersonalBest)
        }

    // ─── Streak display on Home screen ────────────────────────────────────────

    @Test
    fun `HomeViewModel shows current streak correctly after completing daily`() =
        runTest(testDispatcher) {
            // Use real today so HomeViewModel.today matches our pre-populated completions.
            val realToday = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val yesterday = realToday.minus(1, DateTimeUnit.DAY)
            val dayBefore = realToday.minus(2, DateTimeUnit.DAY)

            val dailyRepo = FakeDailyRepository()
            dailyRepo.saveCompletion(realToday)
            dailyRepo.saveCompletion(yesterday)
            dailyRepo.saveCompletion(dayBefore)

            val vm = HomeViewModel(
                thresholdRepository = FakeThresholdRepository(),
                dailyRepository = dailyRepo,
                settingsRepository = FakeSettingsRepository(),
            )
            advanceUntilIdle()

            val state = assertIs<HomeUiState.Ready>(vm.uiState.value)
            assertEquals(3, state.streak)
        }

    // ── Test data factory ─────────────────────────────────────────────────────

    private fun fakeDailyResult(roundsSurvived: Int) = SessionResult(
        gameId = GameId.DAILY,
        deltaE = 2.0f,
        roundsSurvived = roundsSurvived,
        correctRounds = roundsSurvived,
        totalRounds = 6,
        gemsEarned = 3 + roundsSurvived * 5,
        gemBreakdown = emptyList(),
    )
}
