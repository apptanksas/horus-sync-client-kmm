package com.apptank.horus.client

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.apptank.horus.client.config.DATABASE_NAME
import com.apptank.horus.client.database.HorusDatabase
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()


class DatabaseDriverFactory(
    private val context: Context
) : IDatabaseDriverFactory {

    private val driver: SqlDriver = AndroidSqliteDriver(getSchema(), context, getDatabaseName(),
        callback = object : AndroidSqliteDriver.Callback(getSchema()) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }
        })

    override fun createDriver(): SqlDriver {
        return driver
    }

    override fun getDatabase(): HorusDatabase = HorusDatabase(getDatabaseName(), createDriver())
    override fun getDatabaseName(): String = DATABASE_NAME

    override fun getSchema(): HorusDatabase.Schema = HorusDatabase.Schema
}