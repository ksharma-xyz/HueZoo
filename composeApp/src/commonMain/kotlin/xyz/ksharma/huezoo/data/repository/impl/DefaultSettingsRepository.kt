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

    private companion object {
        const val KEY_IS_PAID = "is_paid"
    }
}
