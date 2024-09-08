package com.apptank.horus.client

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.apptank.horus.client.config.DATABASE_NAME
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
        return AndroidSqliteDriver(databaseSchema, context, getDatabaseName(),
            callback = object : AndroidSqliteDriver.Callback(databaseSchema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }

                override fun onCorruption(db: SupportSQLiteDatabase) {
                    super.onCorruption(db)
                }
            })
    }

    override fun retrieveDatabase(): HorusDatabase = HorusDatabase(createDriver())
    override fun getDatabaseName(): String = DATABASE_NAME
}