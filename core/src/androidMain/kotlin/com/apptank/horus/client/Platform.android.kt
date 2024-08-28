package com.apptank.horus.client

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual class DatabaseDriverFactory(
    private val context: Context,
    private val databaseSchema: SqlSchema<QueryResult.Value<Unit>>
) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(databaseSchema, context, "mydatabase.db")
    }
}