package com.apptank.horus.client

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.apptank.horus.client.config.DATABASE_NAME
import com.apptank.horus.client.database.HorusDatabase
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

class DatabaseDriverFactory() : IDatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        val schema = getSchema()
        return NativeSqliteDriver(schema, getDatabaseName(),
            onConfiguration = { config: DatabaseConfiguration ->
                config.copy(
                    extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true)
                )
            })
    }

    override fun getDatabase(): HorusDatabase =  HorusDatabase(getDatabaseName(), createDriver())
    override fun getDatabaseName(): String = DATABASE_NAME

    override fun getSchema(): HorusDatabase.Schema = HorusDatabase.Schema
}