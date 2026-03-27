package xyz.ksharma.huezoo.testutil

import xyz.ksharma.huezoo.data.repository.SettingsRepository

/**
 * In-memory [SettingsRepository] for ViewModel tests.
 *
 * Does not use [kotlinx.coroutines.Dispatchers.Default], so all operations
 * stay on the [kotlinx.coroutines.test.StandardTestDispatcher] and virtual-time
 * control via [kotlinx.coroutines.test.TestScope.advanceUntilIdle] works reliably.
 */
class FakeSettingsRepository(
    private var paid: Boolean = false,
    private var gems: Int = 0,
) : SettingsRepository {

    override suspend fun isPaid(): Boolean = paid
    override suspend fun setPaid(paid: Boolean) { this.paid = paid }

    override suspend fun getGems(): Int = gems
    override suspend fun addGems(amount: Int): Int { gems += amount; return gems }

    override suspend fun hasSeenHealthNotice(): Boolean = false
    override suspend fun setSeenHealthNotice() = Unit

    override suspend fun getUserName(): String? = null
    override suspend fun setUserName(name: String) = Unit

    override suspend fun resetAll() { gems = 0; paid = false }
}
