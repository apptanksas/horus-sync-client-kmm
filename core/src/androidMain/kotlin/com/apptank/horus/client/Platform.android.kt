package com.apptank.horus.client

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import horus.HorusDatabase

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

class DatabaseDriverFactory(
    private val context: Context,
    private val databaseSchema: SqlSchema<QueryResult.Value<Unit>>
) : IDatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(databaseSchema, context, getDatabaseName())
    }

    override fun retrieveDatabase(): HorusDatabase = HorusDatabase(createDriver())
    override fun getDatabaseName(): String = "horus_database"
}

actual fun getPlatformDatabaseDriverFactory(): IDatabaseDriverFactory {
    TODO("Not yet implemented")
}