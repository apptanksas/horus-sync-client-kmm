package org.apptank.horus.client.cache

import org.apptank.horus.client.data.InternalModel
import org.apptank.horus.client.database.struct.Column

/**
 * MemoryCache is a singleton object that stores tables and column names in memory.
 *
 * @author John Ospina
 * @year 2024
 */
internal object MemoryCache {

    // Cache for tables
    private var CACHE_TABLES = mutableMapOf<String, List<InternalModel.TableEntity>>()
    // Cache for column names
    private var CACHE_COLUMN_NAMES = mutableMapOf<String, MutableMap<String, List<Column>>>()

    /**
     * Sets the column names for a table in a database.
     *
     * @param databaseName The name of the database.
     * @param tableName The name of the table.
     * @param columnNames The list of column names.
     */
    fun setTableColumnNames(databaseName: String, tableName: String, columnNames: List<Column>) {

        if (CACHE_COLUMN_NAMES[databaseName] == null)
            CACHE_COLUMN_NAMES[databaseName] = mutableMapOf()

        CACHE_COLUMN_NAMES[databaseName]?.set(tableName, columnNames)
    }

    /**
     * Gets the column names for a table in a database.
     *
     * @param databaseName The name of the database.
     * @param tableName The name of the table.
     * @return The list of column names.
     */
    fun getTableColumnNames(databaseName: String, tableName: String): List<Column>? {
        return CACHE_COLUMN_NAMES[databaseName]?.get(tableName)
    }

    /**
     * Checks if the column names for a table in a database are cached.
     *
     * @param databaseName The name of the database.
     * @param tableName The name of the table.
     * @return True if the column names are cached, false otherwise.
     */
    fun hasTableColumnNames(databaseName: String, tableName: String): Boolean {
        return CACHE_COLUMN_NAMES[databaseName]?.contains(tableName) ?: false
    }

    /**
     * Sets the tables for a database.
     *
     * @param databaseName The name of the database.
     * @param tables The list of tables.
     */
    fun setTables(databaseName: String, tables: List<InternalModel.TableEntity>) {
        CACHE_TABLES[databaseName] = tables
    }

    /**
     * Gets the tables for a database.
     *
     * @param databaseName The name of the database.
     * @return The list of tables.
     */
    fun getTables(databaseName: String): List<InternalModel.TableEntity>? {
        return CACHE_TABLES[databaseName]
    }

    /**
     * Checks if the tables for a database are cached.
     *
     * @param databaseName The name of the database.
     * @return True if the tables are cached, false otherwise.
     */
    fun hasTables(databaseName: String): Boolean {
        return CACHE_TABLES.containsKey(databaseName)
    }

    /**
     * Clears the cached tables and column names.
     */
    fun flushCache() {
        CACHE_TABLES = mutableMapOf()
        CACHE_COLUMN_NAMES = mutableMapOf()
    }
}