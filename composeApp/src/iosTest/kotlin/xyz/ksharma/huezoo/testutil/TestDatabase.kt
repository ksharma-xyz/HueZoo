package xyz.ksharma.huezoo.testutil

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import xyz.ksharma.huezoo.data.db.HuezooDatabase
import kotlin.math.abs
import kotlin.random.Random

/**
 * Creates a unique file-based in-memory database per test.
 *
 * `:memory:` is intentionally avoided because [NativeSqliteDriver] uses separate reader
 * and writer connections, and SQLite in-memory databases are private per connection —
 * writes on the writer would be invisible to reads from a different reader connection.
 *
 * A unique random filename guarantees each test instance gets a clean database, while
 * the file-based driver uses its normal shared connection pool so all operations see
 * the same state. Test binary sandboxing ensures the files are cleaned up by the OS.
 */
actual fun createTestDatabase(): HuezooDatabase {
    val name = "test_${abs(Random.nextInt())}.db"
    return HuezooDatabase(NativeSqliteDriver(HuezooDatabase.Schema, name))
}
