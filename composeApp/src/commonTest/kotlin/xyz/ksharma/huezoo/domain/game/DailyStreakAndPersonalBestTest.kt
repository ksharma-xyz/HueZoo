package xyz.ksharma.huezoo.domain.game

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import xyz.ksharma.huezoo.data.repository.impl.DefaultDailyRepository
import xyz.ksharma.huezoo.testutil.createTestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Daily Challenge streak counting and personal best tracking.
 *
 * Uses a real [DefaultDailyRepository] with an in-memory SQLite database.
 * Each test class instance gets a fresh database (kotlin.test creates a new
 * instance per test method), so there is no shared state between tests.
 *
 * Tests that require [DailyViewModel] / [ResultViewModel] state are marked TODO.
 */
class DailyStreakAndPersonalBestTest {

    private val repo = DefaultDailyRepository(createTestDatabase())

    private val TODAY = LocalDate(2026, 3, 27)
    private val YESTERDAY = LocalDate(2026, 3, 26)

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
    fun `isNewPersonalBest true when correctRounds exceed stored bestRounds`() {
        TODO("needs ResultViewModel: session.correctRounds > stored.bestRounds → isNewPersonalBest = true")
    }

    @Test
    fun `isNewPersonalBest true when no previous personal best exists`() {
        TODO("needs ResultViewModel: null storedBest → isNewPersonalBest = true for any session")
    }

    @Test
    fun `isNewPersonalBest false when correctRounds do not exceed stored bestRounds`() {
        TODO("needs ResultViewModel: session.correctRounds <= stored.bestRounds → isNewPersonalBest = false")
    }

    // ─── Streak display on Home screen ────────────────────────────────────────

    @Test
    fun `HomeViewModel shows current streak correctly after completing daily`() {
        TODO("needs HomeViewModel test harness: complete 3 consecutive days → HomeUiState.streak == 3")
    }
}
