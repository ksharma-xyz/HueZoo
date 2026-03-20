package xyz.ksharma.huezoo.data.repository

interface SettingsRepository {
    suspend fun isPaid(): Boolean
    suspend fun setPaid(paid: Boolean)
    suspend fun getGems(): Int
    suspend fun addGems(amount: Int): Int
}
