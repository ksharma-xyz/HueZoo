package xyz.ksharma.huezoo.data.repository

interface SettingsRepository {
    suspend fun isPaid(): Boolean
    suspend fun setPaid(paid: Boolean)
    suspend fun getGems(): Int
    suspend fun addGems(amount: Int): Int

    /**
     * Returns `true` if the player has already dismissed the eye-strain / health notice.
     * Used to gate first-launch display (UX.5.3) — the notice is shown exactly once,
     * then remains accessible from About / Settings at any time.
     */
    suspend fun hasSeenHealthNotice(): Boolean

    /** Mark the health notice as seen so it is not shown again on next launch. */
    suspend fun setSeenHealthNotice()

    /** Returns the player's display name, or null if not yet set. */
    suspend fun getUserName(): String?

    /** Persists the player's display name. */
    suspend fun setUserName(name: String)

    /** Returns the number of bonus tries currently available (earned via ad or gem spend). */
    suspend fun getBonusTries(): Int

    /** Adds [count] bonus tries and returns the new total. */
    suspend fun addBonusTries(count: Int): Int

    /**
     * Consumes one bonus try.
     * Returns true if a bonus try was available and was deducted; false if balance was 0.
     */
    suspend fun consumeOneBonusTry(): Boolean

    /** Debug only — wipes all game data (sessions, daily, personal bests, settings). */
    suspend fun resetAll()
}
