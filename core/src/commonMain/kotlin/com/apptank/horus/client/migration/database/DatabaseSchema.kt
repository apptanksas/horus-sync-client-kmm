package com.apptank.horus.client.migration.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.apptank.horus.client.database.SQLiteHelper

class DatabaseSchema(
    databaseName: String,
    driver: SqlDriver,
    override val version: Long,
    private val databaseCreatorDelegate: DatabaseTablesCreatorDelegate,
    private val databaseUpgradeDelegate: DatabaseUpgradeDelegate? = null,
) : SQLiteHelper(driver, databaseName), SqlSchema<QueryResult.Value<Unit>> {
    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        databaseCreatorDelegate.createTables {
            driver.execute(null, it, 0)
        }
        return QueryResult.Value(Unit)
    }

    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion
    ): QueryResult.Value<Unit> {
        databaseUpgradeDelegate?.migrate(oldVersion, newVersion) {
            driver.execute(null, it, 0)
        }
        return QueryResult.Value(Unit)
    }

}