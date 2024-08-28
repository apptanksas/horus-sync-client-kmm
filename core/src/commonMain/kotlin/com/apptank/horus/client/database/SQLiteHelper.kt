package com.apptank.horus.client.database

import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.extensions.getRequireBoolean
import com.apptank.horus.client.extensions.getRequireInt
import com.apptank.horus.client.extensions.getRequireString
import com.apptank.horus.client.extensions.notContains
import com.apptank.horus.client.extensions.prepareSQLValueAsString
import com.apptank.horus.client.extensions.rawQuery
import com.apptank.horus.client.extensions.use

abstract class SQLiteHelper(
    protected val driver: SqlDriver,
    private val databaseName: String
) {

    fun getTablesNames(): List<String> {

        if (CACHE_TABLES[databaseName]?.isNotEmpty() == true) {
            return CACHE_TABLES[databaseName] ?: emptyList()
        }

        val tables = mutableListOf<String>()

        this.driver.use {
            // Query tables
            val result: List<String> = it.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'"
            ) { cursor -> cursor.getString(0) }

            tables.addAll(result.filter { TABLES_SYSTEM.notContains(it) })
        }

        return tables.also {
            CACHE_TABLES[databaseName] = it
        }
    }

    fun getColumns(tableName: String): List<Column> {

        if (CACHE_COLUMN_NAMES[databaseName]?.contains(tableName) == true) {
            return CACHE_COLUMN_NAMES[databaseName]?.get(tableName) ?: emptyList()
        }
        val query = "PRAGMA table_info($tableName);" // Query columns

        return driver.use {
            it.rawQuery(query) { cursor ->
                Column(
                    cursor.getRequireInt(0),
                    cursor.getRequireString(1),
                    cursor.getRequireString(2),
                    cursor.getRequireBoolean(3),
                )
            }
        }
    }


    /**
     * Builds a SQL WHERE clause from conditions and an operator.
     *
     * @param conditions The list of conditions.
     * @param operator The logical operator to combine conditions (AND/OR).
     * @return A pair consisting of the WHERE clause and the list of arguments.
     */
    protected fun buildEvaluation(
        conditions: List<WhereCondition>,
        operator: Operator
    ): Pair<String, List<String>> {
        return Pair(
            conditions.joinToString(
                operator.name,
                transform = { " ${it.columnValue.column} ${it.comparator} ? " }
            ).trim(),
            conditions.map {
                it.columnValue.value.prepareSQLValueAsString()
                    .replace("\"", "")
            }
        )
    }

    companion object {
        private val TABLES_SYSTEM = listOf("android_metadata", "sqlite_sequence")
        private var CACHE_TABLES = mutableMapOf<String, List<String>>()
        private var CACHE_COLUMN_NAMES = mutableMapOf<String, MutableMap<String, List<Column>>>()

        fun flushCache() {
            CACHE_TABLES = mutableMapOf()
            CACHE_COLUMN_NAMES = mutableMapOf()
        }
    }


}