package com.apptank.horus.client

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.migration.database.DatabaseSchema
import horus.HorusDatabase
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

class DatabaseDriverFactory(
    private val databaseSchema: SqlSchema<QueryResult.Value<Unit>>
) : IDatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(databaseSchema, getDatabaseName())
    }

    override fun retrieveDatabase(): HorusDatabase = HorusDatabase(createDriver())
    override fun getDatabaseName(): String = "horus_database"
}

actual fun getPlatformDatabaseDriverFactory(): IDatabaseDriverFactory {
    TODO("Not yet implemented")
}