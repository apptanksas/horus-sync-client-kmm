package org.apptank.horus.client

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.apptank.horus.client.config.DATABASE_NAME
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.di.IDatabaseDriverFactory

internal class DatabaseDriverFactory(
    context: Context
) : IDatabaseDriverFactory {

    private val driver: SqlDriver = AndroidSqliteDriver(getSchema(), context, getDatabaseName(),
        callback = object : AndroidSqliteDriver.Callback(getSchema()) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }
        })

    override fun getDriver(): SqlDriver {
        return driver
    }

    override fun getDatabase(): HorusDatabase = HorusDatabase(getDatabaseName(), getDriver())
    override fun getDatabaseName(): String = DATABASE_NAME

    override fun getSchema(): HorusDatabase.Schema = HorusDatabase.Schema
}