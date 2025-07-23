package org.apptank.horus.client.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import org.apptank.horus.client.cache.MemoryCache
import org.apptank.horus.client.control.scheme.EntitiesTable
import org.apptank.horus.client.control.QueueActionsTable
import org.apptank.horus.client.control.scheme.DataSharedTable
import org.apptank.horus.client.control.scheme.EntityAttributesTable
import org.apptank.horus.client.control.scheme.SyncControlTable
import org.apptank.horus.client.control.scheme.SyncFileTable
import org.apptank.horus.client.extensions.createSQLInsert
import org.apptank.horus.client.extensions.execute
import org.apptank.horus.client.extensions.getRequireInt
import org.apptank.horus.client.extensions.handle
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.logException
import org.apptank.horus.client.migration.database.DatabaseTablesCreatorDelegate
import org.apptank.horus.client.migration.database.DatabaseUpgradeDelegate
import org.apptank.horus.client.migration.domain.EntityScheme
import org.apptank.horus.client.migration.domain.getLastVersion
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
                execute(SyncFileTable.SQL_CREATE_TABLE)
                execute(EntityAttributesTable.SQL_CREATE_TABLE)
                execute(DataSharedTable.SQL_CREATE_TABLE)

                databaseCreatorDelegate?.createTables {
                    execute(Random.nextInt(), it, 0)
                    info("[Migration] Created table: $it")
                }

                // Insert entities into the entities table indicating if they are writable
                databaseCreatorDelegate?.getEntitiesCreated()?.forEach { entity ->
                    insertEntity(entity)
                }
            }
            MemoryCache.flushCache()
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

                databaseUpgradeDelegate?.migrate(oldVersion, newVersion) { sql, entity, attribute ->
                    driver.execute(Random.nextInt(), sql, 0)
                    info("[Migration: Alter] Executed: $sql")

                    // Insert new attributes into the entity attributes table usually for new attributes
                    attribute?.let {
                        execute(
                            createSQLInsert(
                                EntityAttributesTable.TABLE_NAME,
                                EntityAttributesTable.mapToCreate(entity, it.name, it.type)
                            )
                        )
                    }
                }

                // Insert entities into the entities table indicating if they are writable
                databaseUpgradeDelegate?.getNewEntitiesCreated()?.forEach { entity ->
                    insertEntity(entity)
                }
            }
            MemoryCache.flushCache()
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

                if (entityNameExists(entity.name)) {
                    return
                }

                execute(
                    createSQLInsert(
                        EntitiesTable.TABLE_NAME,
                        EntitiesTable.mapToCreate(entity.name, entity.isWritable(), entity.level)
                    )
                )
                entity.attributes.forEach {
                    execute(
                        createSQLInsert(
                            EntityAttributesTable.TABLE_NAME,
                            EntityAttributesTable.mapToCreate(entity.name, it.name, it.type)
                        )
                    )
                }
            } catch (e: Exception) {
                logException("[Migration] Error insert entity", e)
            }
        }


        /**
         * Validates if an entity name exists in the database.
         *
         * @param entityName The name of the entity to validate.
         * @return True if the entity name exists, false otherwise.
         */
        private fun SqlDriver.entityNameExists(entityName: String): Boolean {
            return executeQuery(
                null,
                "SELECT COUNT(*) FROM ${EntitiesTable.TABLE_NAME} WHERE ${EntitiesTable.ATTR_NAME} = '$entityName'", {
                    if (!it.next().value) {
                        return@executeQuery QueryResult.Value(0)
                    }
                    val count = it.getLong(0) ?: 0
                    QueryResult.Value(count)
                },
                0
            ).value > 0
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
