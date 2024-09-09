package com.apptank.horus.client.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.apptank.horus.client.control.QueueActionsTable
import com.apptank.horus.client.control.SyncControlTable
import com.apptank.horus.client.extensions.execute
import com.apptank.horus.client.extensions.handle
import com.apptank.horus.client.migration.database.DatabaseTablesCreatorDelegate
import com.apptank.horus.client.migration.database.DatabaseUpgradeDelegate
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.domain.getLastVersion
import kotlin.random.Random

class HorusDatabase(
    databaseName: String,
    driver: SqlDriver
) : SQLiteHelper(driver, databaseName) {

    object Schema : SqlSchema<QueryResult.Value<Unit>> {

        var databaseCreatorDelegate: DatabaseTablesCreatorDelegate? = null
        var databaseUpgradeDelegate: DatabaseUpgradeDelegate? = null

        var currentVersion: Long = 1

        override val version: Long
            get() {
                return currentVersion
            }

        fun create(
            driver: SqlDriver,
            schemes: List<EntityScheme>
        ): QueryResult.Value<Unit> {
            databaseCreatorDelegate = DatabaseTablesCreatorDelegate(schemes)
            currentVersion = schemes.getLastVersion()
            return create(driver)
        }

        private fun <T> SqlDriver.rawQuery(query: String, mapper: (SqlCursor) -> T?): List<T> {
            return executeQuery(null, query, {
                val resultList = mutableListOf<T>()
                while (it.next().value) {
                    mapper(it)?.let { item ->
                        resultList.add(item)
                    }
                }
                QueryResult.Value(resultList)
            }, 0).value
        }


        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.handle {
                databaseCreatorDelegate?.createTables {
                    execute(Random.nextInt(), it, 0)
                }
                execute(SyncControlTable.SQL_CREATE_TABLE)
                execute(QueueActionsTable.SQL_CREATE_TABLE)
            }
            flushCache()
            return QueryResult.Value(Unit)
        }

        fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            schemes: List<EntityScheme>,
            vararg callbacks: AfterVersion
        ): QueryResult.Value<Unit> {
            databaseUpgradeDelegate = DatabaseUpgradeDelegate(schemes)
            return migrate(driver, oldVersion, newVersion, *callbacks)
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion
        ): QueryResult.Value<Unit> {
            databaseUpgradeDelegate?.migrate(oldVersion, newVersion) {
                driver.handle {
                    driver.execute(Random.nextInt(), it, 0)
                }
            }
            flushCache()
            callbacks.forEach {
                if (newVersion >= it.afterVersion) {
                    it.block(driver)
                }
            }
            currentVersion = newVersion
            return QueryResult.Value(Unit)
        }
    }

    fun getDatabaseDriver(): SqlDriver {
        return driver
    }
}