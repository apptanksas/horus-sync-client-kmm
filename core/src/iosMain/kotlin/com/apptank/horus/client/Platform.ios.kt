package com.apptank.horus.client

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.apptank.horus.client.config.DATABASE_NAME
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import horus.HorusDatabase

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

class DatabaseDriverFactory(
    private val databaseSchema: SqlSchema<QueryResult.Value<Unit>>
) : IDatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(databaseSchema, getDatabaseName(),
            onConfiguration = { config: DatabaseConfiguration ->
                config.copy(
                    extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true)
                )
            })
    }

    override fun retrieveDatabase(): HorusDatabase = HorusDatabase(createDriver())
    override fun getDatabaseName(): String = DATABASE_NAME
}