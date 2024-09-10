package com.apptank.horus.client.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.apptank.horus.client.control.EntitiesTable
import com.apptank.horus.client.control.QueueActionsTable
import com.apptank.horus.client.control.SyncControlTable
import com.apptank.horus.client.extensions.createSQLInsert
import com.apptank.horus.client.extensions.execute
import com.apptank.horus.client.extensions.handle
import com.apptank.horus.client.migration.database.DatabaseTablesCreatorDelegate
import com.apptank.horus.client.migration.database.DatabaseUpgradeDelegate
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.domain.getLastVersion
import kotlin.random.Random

/**
 * Represents a database helper for Horus operations, extending [SQLiteHelper].
 *
 * @param databaseName The name of the database.
 * @param driver The SQL driver used to interact with the database.
 */
class HorusDatabase(
    databaseName: String,
    driver: SqlDriver
) : SQLiteHelper(driver, databaseName) {

    /**
     * Schema object for creating and migrating the database schema.
     */
    object Schema : SqlSchema<QueryResult.Value<Unit>> {

        // Delegate for creating database tables
        var databaseCreatorDelegate: DatabaseTablesCreatorDelegate? = null

        // Delegate for upgrading the database schema
        var databaseUpgradeDelegate: DatabaseUpgradeDelegate? = null

        // Current version of the database schema
        var currentVersion: Long = 1

        /**
         * Gets the current version of the database schema.
         */
        override val version: Long
            get() {
                return currentVersion
            }

        /**
         * Creates the database schema using the provided driver and entity schemes.
         *
         * @param driver The SQL driver used to execute queries.
         * @param schemes List of [EntityScheme] representing the database schema.
         * @return A [QueryResult.Value] indicating the result of the creation operation.
         */
        fun create(
            driver: SqlDriver,
            schemes: List<EntityScheme>
        ): QueryResult.Value<Unit> {
            databaseCreatorDelegate = DatabaseTablesCreatorDelegate(schemes)
            currentVersion = schemes.getLastVersion()
            return create(driver)
        }

        /**
         * Executes a raw query using the provided driver and maps the results using the provided mapper.
         *
         * @param query The SQL query to execute.
         * @param mapper A function to map each row of the cursor to a result item.
         * @return A list of results obtained from the query.
         */
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

        /**
         * Creates the database schema using the provided driver.
         *
         * @param driver The SQL driver used to execute queries.
         * @return A [QueryResult.Value] indicating the result of the creation operation.
         */
        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.handle {

                execute(EntitiesTable.SQL_CREATE_TABLE)
                execute(SyncControlTable.SQL_CREATE_TABLE)
                execute(QueueActionsTable.SQL_CREATE_TABLE)

                databaseCreatorDelegate?.createTables {
                    execute(Random.nextInt(), it, 0)
                }

                // Insert entities into the entities table indicating if they are writable
                databaseCreatorDelegate?.getEntitiesCreated()?.forEach { entity ->
                    insertEntity(entity)
                }
            }
            flushCache()
            return QueryResult.Value(Unit)
        }

        /**
         * Migrates the database schema from an old version to a new version.
         *
         * @param driver The SQL driver used to execute queries.
         * @param oldVersion The old version of the database schema.
         * @param newVersion The new version of the database schema.
         * @param schemes List of [EntityScheme] representing the new database schema.
         * @param callbacks Vararg [AfterVersion] callbacks to be executed after migration.
         * @return A [QueryResult.Value] indicating the result of the migration operation.
         */
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

        /**
         * Migrates the database schema from an old version to a new version.
         *
         * @param driver The SQL driver used to execute queries.
         * @param oldVersion The old version of the database schema.
         * @param newVersion The new version of the database schema.
         * @param callbacks Vararg [AfterVersion] callbacks to be executed after migration.
         * @return A [QueryResult.Value] indicating the result of the migration operation.
         */
        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion
        ): QueryResult.Value<Unit> {
            driver.handle {
                databaseUpgradeDelegate?.migrate(oldVersion, newVersion) {
                    driver.execute(Random.nextInt(), it, 0)
                }

                // Insert entities into the entities table indicating if they are writable
                databaseUpgradeDelegate?.getNewEntitiesCreated()?.forEach { entity ->
                    insertEntity(entity)
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

        private fun SqlDriver.insertEntity(entity: EntityScheme) {
            try {
                execute(
                    createSQLInsert(
                        EntitiesTable.TABLE_NAME,
                        EntitiesTable.mapToCreate(entity.name, entity.isWritable())
                    )
                )
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Gets the SQL driver associated with this database.
     *
     * @return The [SqlDriver] used to interact with the database.
     */
    fun getDatabaseDriver(): SqlDriver {
        return driver
    }

}
