package xyz.ksharma.huezoo.domain.game

import kotlinx.coroutines.runBlocking
import xyz.ksharma.huezoo.data.repository.impl.DefaultSettingsRepository
import xyz.ksharma.huezoo.data.repository.impl.DefaultThresholdRepository
import xyz.ksharma.huezoo.domain.game.model.AttemptStatus
import xyz.ksharma.huezoo.testutil.FakePlatformOps
import xyz.ksharma.huezoo.testutil.createTestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Tests for the Threshold 8-hour attempt window and paid-user bypass.
 *
 * Uses a real [DefaultThresholdRepository] backed by an in-memory SQLite database
 * so every call exercises the actual SQL queries and business logic.
 *
 * Time is controlled by passing explicit [Instant] values — no real-time dependencies.
 * Each test gets a fresh database (kotlin.test creates a new class instance per test).
 */
@OptIn(ExperimentalTime::class)
class ThresholdAttemptWindowTest {

    private val db = createTestDatabase()
    private val settingsRepo = DefaultSettingsRepository(db)
    private val repo = DefaultThresholdRepository(db, FakePlatformOps(), settingsRepo)

    /** Anchor point in time — all test timestamps are expressed relative to this. */
    private val T0 = Instant.fromEpochSeconds(0)

    // ─── Fresh start ──────────────────────────────────────────────────────────

    @Test
    fun `status is Available with 0 used attempts before any attempt is recorded`() = runBlocking {
        val status = assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0))
        assertEquals(0, status.attemptsUsed)
        assertEquals(ThresholdGameEngine.MAX_ATTEMPTS, status.maxAttempts)
    }

    @Test
    fun `recording first attempt creates an 8-hour window`() = runBlocking {
        repo.recordAttempt(T0)

        // Immediately after: 1 attempt used, still available
        val atT0 = assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0))
        assertEquals(1, atT0.attemptsUsed)

        // 7 h 59 m later: window still active
        val atT7h59m = assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0 + 7.hours + 59.minutes))
        assertEquals(1, atT7h59m.attemptsUsed)

        // 8 h 01 m later: window expired → slate wiped, back to 0 used
        val atT8h1m = assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0 + 8.hours + 1.minutes))
        assertEquals(0, atT8h1m.attemptsUsed)
    }

    // ─── Attempt counting ─────────────────────────────────────────────────────

    @Test
    fun `each recorded attempt increments attemptsUsed by one`() = runBlocking {
        repeat(3) { repo.recordAttempt(T0) }
        val status = assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0))
        assertEquals(3, status.attemptsUsed)
    }

    @Test
    fun `fifth attempt exhausts the free window`() = runBlocking {
        repeat(ThresholdGameEngine.MAX_ATTEMPTS) { repo.recordAttempt(T0) }
        assertIs<AttemptStatus.Exhausted>(repo.getAttemptStatus(T0))
        Unit
    }

    @Test
    fun `sixth attempt within the window does not increment beyond exhausted`() = runBlocking {
        // Record one extra attempt beyond max — repo must stay safe
        // (ThresholdViewModel guards against this in normal usage)
        repeat(ThresholdGameEngine.MAX_ATTEMPTS + 1) { repo.recordAttempt(T0) }
        assertIs<AttemptStatus.Exhausted>(repo.getAttemptStatus(T0))
        Unit
    }

    // ─── Window expiry ────────────────────────────────────────────────────────

    @Test
    fun `expired window is deleted and returns fresh Available status`() = runBlocking {
        repeat(3) { repo.recordAttempt(T0) }

        val statusAfterExpiry = assertIs<AttemptStatus.Available>(
            repo.getAttemptStatus(T0 + 8.hours + 1.minutes),
        )
        assertEquals(0, statusAfterExpiry.attemptsUsed)
    }

    @Test
    fun `exhausted window expires and restores full attempts after 8 hours`() = runBlocking {
        repeat(ThresholdGameEngine.MAX_ATTEMPTS) { repo.recordAttempt(T0) }

        val statusAfterExpiry = assertIs<AttemptStatus.Available>(
            repo.getAttemptStatus(T0 + 8.hours + 1.minutes),
        )
        assertEquals(0, statusAfterExpiry.attemptsUsed)
    }

    // ─── Paid user bypass ─────────────────────────────────────────────────────

    @Test
    fun `paid user gets Available status even after 5 attempts are used`() = runBlocking {
        settingsRepo.setPaid(true)
        repeat(ThresholdGameEngine.MAX_ATTEMPTS) { repo.recordAttempt(T0) }

        val status = assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0))
        assertEquals(0, status.attemptsUsed)
    }

    @Test
    fun `paid user can start unlimited sessions within same 8-hour period`() = runBlocking {
        settingsRepo.setPaid(true)
        // Exhaust and auto-reset twice in the same window — no cooldown for paid users
        repeat(2) {
            repeat(ThresholdGameEngine.MAX_ATTEMPTS) { repo.recordAttempt(T0) }
            val status = assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0))
            assertEquals(0, status.attemptsUsed)
        }
    }

    @Test
    fun `free user does NOT bypass exhaustion regardless of attempt count`() = runBlocking {
        repeat(ThresholdGameEngine.MAX_ATTEMPTS) { repo.recordAttempt(T0) }
        val status = assertIs<AttemptStatus.Exhausted>(repo.getAttemptStatus(T0))

        // nextResetAt must be 8 hours from the first attempt at T0
        val expectedReset = T0 + 8.hours
        val drift = (status.nextResetAt - expectedReset).absoluteValue
        assertTrue(drift < 1.minutes, "nextResetAt should be ~8h from T0, drift was $drift")
    }

    // ─── Bonus try availability (regression: earn→play→earn cycle) ───────────

    @Test
    fun `bonus try makes status Available when base tries are exhausted`() = runBlocking {
        // Use all base tries
        repeat(ThresholdGameEngine.MAX_ATTEMPTS) { repo.recordAttempt(T0) }
        assertIs<AttemptStatus.Exhausted>(repo.getAttemptStatus(T0))

        // Grant one bonus try
        settingsRepo.addBonusTries(1)

        // Now must be Available
        val status = assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0))
        // attemptsUsed reported as maxAttempts - visualRemaining = maxAttempts - 1
        assertEquals(ThresholdGameEngine.MAX_ATTEMPTS - 1, status.attemptsUsed)
    }

    @Test
    fun `earn-play-earn cycle stays Available (regression for double-shrink formula)`() = runBlocking {
        // Use all base tries
        repeat(ThresholdGameEngine.MAX_ATTEMPTS) { repo.recordAttempt(T0) }

        // Earn a bonus try (e.g. watch ad)
        settingsRepo.addBonusTries(1)
        assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0))

        // Play the bonus game — recordAttempt increments attemptsUsed AND consumeOneBonusTry
        repo.recordAttempt(T0)
        // bonusTries is now 0, attemptsUsed > maxAttempts → old formula would say Exhausted
        assertIs<AttemptStatus.Exhausted>(repo.getAttemptStatus(T0))

        // Earn again
        settingsRepo.addBonusTries(1)
        // Must be Available again — old "attemptsUsed < maxAttempts + bonusTries" formula fails
        // here because attemptsUsed = maxAttempts+1 and bonusTries = 1, so effectiveMax=maxAttempts+1
        // which equals attemptsUsed → Exhausted (bug). New formula: hasBonusTries → Available.
        assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0))
    }

    @Test
    fun `multiple bonus tries all register as Available`() = runBlocking {
        repeat(ThresholdGameEngine.MAX_ATTEMPTS) { repo.recordAttempt(T0) }
        settingsRepo.addBonusTries(3)

        val status = assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0))
        // visualRemaining = minOf(bonusTries=3, maxAttempts) = 3
        // attemptsUsed reported = maxAttempts - 3
        assertEquals(ThresholdGameEngine.MAX_ATTEMPTS - 3, status.attemptsUsed)
        assertEquals(ThresholdGameEngine.MAX_ATTEMPTS, status.maxAttempts)
    }

    @Test
    fun `bonus tries cap visual hearts at maxAttempts`() = runBlocking {
        repeat(ThresholdGameEngine.MAX_ATTEMPTS) { repo.recordAttempt(T0) }
        // More bonus tries than max hearts — visual cap kicks in
        settingsRepo.addBonusTries(ThresholdGameEngine.MAX_ATTEMPTS + 5)

        val status = assertIs<AttemptStatus.Available>(repo.getAttemptStatus(T0))
        // visualRemaining = minOf(bonusTries, maxAttempts) = maxAttempts → attemptsUsed = 0
        assertEquals(0, status.attemptsUsed)
        assertEquals(ThresholdGameEngine.MAX_ATTEMPTS, status.maxAttempts)
    }

    @Test
    fun `exhausting all bonus tries returns to Exhausted`() = runBlocking {
        repeat(ThresholdGameEngine.MAX_ATTEMPTS) { repo.recordAttempt(T0) }
        settingsRepo.addBonusTries(2)

        // Play both bonus games
        repeat(2) { repo.recordAttempt(T0) }

        // bonusTries=0, attemptsUsed > maxAttempts → Exhausted
        assertIs<AttemptStatus.Exhausted>(repo.getAttemptStatus(T0))
        Unit
    }
}
