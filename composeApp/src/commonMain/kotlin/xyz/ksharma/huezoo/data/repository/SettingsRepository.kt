package xyz.ksharma.huezoo.data.repository

interface SettingsRepository {
    suspend fun isPaid(): Boolean
    suspend fun setPaid(paid: Boolean)
    suspend fun getGems(): Int
    suspend fun addGems(amount: Int): Int

    /** Debug only — wipes all game data (sessions, daily, personal bests, settings). */
    suspend fun resetAll()
}
