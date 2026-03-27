package xyz.ksharma.huezoo.testutil

import xyz.ksharma.huezoo.data.db.HuezooDatabase

/**
 * Creates a fresh in-memory [HuezooDatabase] for each test.
 *
 * Each call returns an independent database instance — no state leaks between tests.
 * Platform-specific `actual` implementations wire the appropriate SQLDelight driver.
 */
expect fun createTestDatabase(): HuezooDatabase
