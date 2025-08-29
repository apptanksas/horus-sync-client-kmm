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

    // Cache for tables entities
    private var CACHE_TABLES = listOf<String>()
    private var CACHE_TABLES_ENTITIES = mutableMapOf<String, List<InternalModel.TableEntity>>()
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
    fun setTablesEntities(databaseName: String, tables: List<InternalModel.TableEntity>) {
        CACHE_TABLES_ENTITIES[databaseName] = tables
    }

    /**
     * Gets the tables for a database.
     *
     * @param databaseName The name of the database.
     * @return The list of tables.
     */
    fun getTablesEntities(databaseName: String): List<InternalModel.TableEntity>? {
        return CACHE_TABLES_ENTITIES[databaseName]
    }

    /**
     * Checks if the tables for a database are cached.
     *
     * @param databaseName The name of the database.
     * @return True if the tables are cached, false otherwise.
     */
    fun hasTablesEntities(databaseName: String): Boolean {
        return CACHE_TABLES_ENTITIES.containsKey(databaseName)
    }

    /**
     * Sets the list of tables.
     *
     * @param tables The list of tables.
     */
    fun setTables(tables: List<String>) {
        CACHE_TABLES = tables
    }

    /**
     * Gets the list of tables.
     *
     * @return The list of tables.
     */
    fun getTables(): List<String> {
        return CACHE_TABLES
    }

    /**
     * Checks if the list of tables is cached.
     *
     * @return True if the list of tables is cached, false otherwise.
     */
    fun hasTables(): Boolean {
        return CACHE_TABLES.isNotEmpty()
    }


    /**
     * Clears the cached tables and column names.
     */
    fun flushCache() {
        CACHE_TABLES = mutableListOf()
        CACHE_TABLES_ENTITIES = mutableMapOf()
        CACHE_COLUMN_NAMES = mutableMapOf()
    }
}