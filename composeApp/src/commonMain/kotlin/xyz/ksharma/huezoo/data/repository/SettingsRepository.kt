package xyz.ksharma.huezoo.data.repository

interface SettingsRepository {
    suspend fun isPaid(): Boolean
    suspend fun setPaid(paid: Boolean)
    suspend fun getGems(): Int
    suspend fun addGems(amount: Int): Int

    /** Returns true if the user has already dismissed the first-launch ΔE info card. */
    suspend fun hasDismissedDeltaECard(): Boolean

    /** Marks the ΔE info card as permanently dismissed. */
    suspend fun dismissDeltaECard()

    /** Debug only — wipes all game data (sessions, daily, personal bests, settings). */
    suspend fun resetAll()
}
