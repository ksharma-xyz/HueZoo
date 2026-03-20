package xyz.ksharma.huezoo.data.repository.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.ksharma.huezoo.data.db.HuezooDatabase
import xyz.ksharma.huezoo.data.repository.SettingsRepository

class DefaultSettingsRepository(
    private val db: HuezooDatabase,
) : SettingsRepository {

    override suspend fun isPaid(): Boolean = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getSetting(KEY_IS_PAID).executeAsOneOrNull()?.toBoolean() ?: false
    }

    override suspend fun setPaid(paid: Boolean) = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.setSetting(KEY_IS_PAID, paid.toString())
        Unit
    }

    override suspend fun getGems(): Int = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getSetting(KEY_TOTAL_GEMS).executeAsOneOrNull()?.toIntOrNull() ?: 0
    }

    override suspend fun addGems(amount: Int): Int = withContext(Dispatchers.Default) {
        val current = db.huezooDatabaseQueries.getSetting(KEY_TOTAL_GEMS).executeAsOneOrNull()?.toIntOrNull() ?: 0
        val next = current + amount
        db.huezooDatabaseQueries.setSetting(KEY_TOTAL_GEMS, next.toString())
        next
    }

    override suspend fun hasDismissedDeltaECard(): Boolean = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getSetting(KEY_DELTA_E_CARD_DISMISSED).executeAsOneOrNull()?.toBoolean() ?: false
    }

    override suspend fun dismissDeltaECard() = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.setSetting(KEY_DELTA_E_CARD_DISMISSED, "true")
        Unit
    }

    override suspend fun resetAll() = withContext(Dispatchers.Default) {
        val q = db.huezooDatabaseQueries
        q.deleteAllThresholdSessions()
        q.deleteAllDailyChallenges()
        q.deleteAllPersonalBests()
        q.deleteAllSettings()
        Unit
    }

    private companion object {
        const val KEY_IS_PAID = "is_paid"
        const val KEY_TOTAL_GEMS = "total_gems"
        const val KEY_DELTA_E_CARD_DISMISSED = "delta_e_card_dismissed"
    }
}
