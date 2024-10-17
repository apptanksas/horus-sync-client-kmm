package org.apptank.horus.client.cache

import org.apptank.horus.client.data.InternalModel
import org.apptank.horus.client.database.struct.Column

internal object MemoryCache {

    private var CACHE_TABLES = mutableMapOf<String, List<InternalModel.TableEntity>>()
    private var CACHE_COLUMN_NAMES = mutableMapOf<String, MutableMap<String, List<Column>>>()

    fun setTableColumnNames(databaseName: String, tableName: String, columnNames: List<Column>) {

        if (CACHE_COLUMN_NAMES[databaseName] == null)
            CACHE_COLUMN_NAMES[databaseName] = mutableMapOf()

        CACHE_COLUMN_NAMES[databaseName]?.set(tableName, columnNames)
    }

    fun getTableColumnNames(databaseName: String, tableName: String): List<Column>? {
        return CACHE_COLUMN_NAMES[databaseName]?.get(tableName)
    }

    fun hasTableColumnNames(databaseName: String, tableName: String): Boolean {
        return CACHE_COLUMN_NAMES[databaseName]?.contains(tableName) ?: false
    }

    fun setTables(databaseName: String, tables: List<InternalModel.TableEntity>) {
        CACHE_TABLES[databaseName] = tables
    }

    fun getTables(databaseName: String): List<InternalModel.TableEntity>? {
        return CACHE_TABLES[databaseName]
    }

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