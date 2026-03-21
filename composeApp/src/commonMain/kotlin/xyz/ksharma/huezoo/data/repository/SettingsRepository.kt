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

    /** Debug only — wipes all game data (sessions, daily, personal bests, settings). */
    suspend fun resetAll()
}
