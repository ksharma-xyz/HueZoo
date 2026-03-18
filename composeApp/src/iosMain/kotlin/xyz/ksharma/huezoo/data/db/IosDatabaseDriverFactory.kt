package xyz.ksharma.huezoo.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = HuezooDatabase.Schema,
        name = "huezoo.db",
    )
}
