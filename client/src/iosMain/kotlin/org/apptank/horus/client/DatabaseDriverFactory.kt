package org.apptank.horus.client

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import org.apptank.horus.client.config.DATABASE_NAME
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.di.IDatabaseDriverFactory

/**
 * A factory class responsible for creating and providing database drivers
 * and instances of the `HorusDatabase`.
 */
class DatabaseDriverFactory() : IDatabaseDriverFactory {

    // Creates a NativeSqliteDriver with the provided database schema and name,
    // and configures the database to support foreign key constraints.
    private val driver = NativeSqliteDriver(getSchema(), getDatabaseName(),
        onConfiguration = { config: DatabaseConfiguration ->
            config.copy(
                extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true)
            )
        })

    /**
     * Provides an instance of the `SqlDriver` for database operations.
     *
     * @return The `SqlDriver` instance used for database interaction.
     */
    override fun getDriver(): SqlDriver {
        return driver
    }

    /**
     * Provides an instance of the `HorusDatabase` using the specified database name and driver.
     *
     * @return The `HorusDatabase` instance representing the database.
     */
    override fun getDatabase(): HorusDatabase = HorusDatabase(getDatabaseName(), getDriver())

    /**
     * Returns the name of the database.
     *
     * @return The name of the database as a string.
     */
    override fun getDatabaseName(): String = DATABASE_NAME

    /**
     * Provides the schema definition for the `HorusDatabase`.
     *
     * @return The schema of the database.
     */
    override fun getSchema(): HorusDatabase.Schema = HorusDatabase.Schema
}
