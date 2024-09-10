package com.apptank.horus.client.di

import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.database.HorusDatabase

/**
 * Interface for a factory responsible for creating and providing database-related objects.
 *
 * This interface defines methods to obtain the database name, access the database schema,
 * create a database driver, and retrieve the database instance.
 */
interface IDatabaseDriverFactory {

    /**
     * Returns the name of the database.
     *
     * @return A string representing the name of the database.
     */
    fun getDatabaseName(): String

    /**
     * Retrieves an instance of the `HorusDatabase`.
     *
     * @return An instance of `HorusDatabase`.
     */
    fun getDatabase(): HorusDatabase

    /**
     * Provides the schema of the `HorusDatabase`.
     *
     * @return An instance of `HorusDatabase.Schema` representing the database schema.
     */
    fun getSchema(): HorusDatabase.Schema

    /**
     * Retrieves a new instance of `SqlDriver`.
     *
     * @return A new instance of `SqlDriver`.
     */
    fun getDriver(): SqlDriver
}
