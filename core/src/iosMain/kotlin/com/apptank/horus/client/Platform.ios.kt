package com.apptank.horus.client

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.apptank.horus.client.migration.database.DatabaseSchema
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual class DatabaseDriverFactory(
    private val databaseSchema: SqlSchema<QueryResult.Value<Unit>>
) {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(databaseSchema, "mydatabase.db")
    }
}