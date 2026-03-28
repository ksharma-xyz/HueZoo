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

    override suspend fun hasSeenHealthNotice(): Boolean = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getSetting(KEY_HEALTH_NOTICE_SEEN)
            .executeAsOneOrNull()
            ?.toBoolean() ?: false
    }

    override suspend fun setSeenHealthNotice() = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.setSetting(KEY_HEALTH_NOTICE_SEEN, "true")
        Unit
    }

    override suspend fun getUserName(): String? = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getSetting(KEY_USER_NAME).executeAsOneOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    override suspend fun setUserName(name: String) = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.setSetting(KEY_USER_NAME, name.trim())
        Unit
    }

    override suspend fun getBonusTries(): Int = withContext(Dispatchers.Default) {
        db.huezooDatabaseQueries.getSetting(KEY_BONUS_TRIES).executeAsOneOrNull()?.toIntOrNull() ?: 0
    }

    override suspend fun addBonusTries(count: Int): Int = withContext(Dispatchers.Default) {
        val current = db.huezooDatabaseQueries.getSetting(KEY_BONUS_TRIES).executeAsOneOrNull()?.toIntOrNull() ?: 0
        val next = current + count
        db.huezooDatabaseQueries.setSetting(KEY_BONUS_TRIES, next.toString())
        next
    }

    override suspend fun consumeOneBonusTry(): Boolean = withContext(Dispatchers.Default) {
        val current = db.huezooDatabaseQueries.getSetting(KEY_BONUS_TRIES).executeAsOneOrNull()?.toIntOrNull() ?: 0
        if (current <= 0) return@withContext false
        db.huezooDatabaseQueries.setSetting(KEY_BONUS_TRIES, (current - 1).toString())
        true
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
        const val KEY_HEALTH_NOTICE_SEEN = "health_notice_seen"
        const val KEY_USER_NAME = "user_name"
        const val KEY_BONUS_TRIES = "bonus_tries"
    }
}
