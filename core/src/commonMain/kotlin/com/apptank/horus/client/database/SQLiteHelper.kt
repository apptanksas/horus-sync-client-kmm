package com.apptank.horus.client.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.base.MapAttributes
import com.apptank.horus.client.extensions.getRequireBoolean
import com.apptank.horus.client.extensions.getRequireDouble
import com.apptank.horus.client.extensions.getRequireInt
import com.apptank.horus.client.extensions.getRequireString
import com.apptank.horus.client.extensions.notContains
import com.apptank.horus.client.extensions.prepareSQLValueAsString
import com.apptank.horus.client.extensions.handle
import com.apptank.horus.client.extensions.info
import com.apptank.horus.client.extensions.log

abstract class SQLiteHelper(
    protected val driver: SqlDriver,
    private val databaseName: String
) {

    fun getTablesNames(): List<String> {

        if (CACHE_TABLES[databaseName]?.isNotEmpty() == true) {
            return CACHE_TABLES[databaseName] ?: emptyList()
        }

        val tables = mutableListOf<String>()

        this.driver.handle {
            // Query tables
            val result: List<String> = rawQuery(
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

        return driver.handle {
            rawQuery(query) { cursor ->
                Column(
                    cursor.getRequireInt(0),
                    cursor.getRequireString(1),
                    cursor.getRequireString(2),
                    cursor.getRequireBoolean(3),
                )
            }
        }
    }


    protected fun <T> rawQuery(query: String, mapper: (SqlCursor) -> T?): List<T> {

        return driver.executeQuery(null, query, {
            val resultList = mutableListOf<T>()
            while (it.next().value) {
                mapper(it)?.let { item ->
                    resultList.add(item)
                }
            }
            QueryResult.Value(resultList)
        }, 0).value
    }

    protected fun <T> queryResult(query: String, mapper: (Cursor) -> T?): List<T> {

        val tableName = getTableName(query)

        return driver.executeQuery(null, query, {
            val resultList = mutableListOf<T>()
            buildCursorValues(tableName, it).forEach { cursor ->
                mapper(cursor)?.let { item ->
                    resultList.add(item)
                }
            }
            QueryResult.Value(resultList)
        }, 0).value
    }


    protected fun insertOrThrow(table: String, values: MapAttributes) {
        val columns = values.keys.joinToString(", ")
        val valuesString = values.values.joinToString(", ") { it.prepareSQLValueAsString() }
        val query = "INSERT INTO $table ($columns) VALUES ($valuesString);"
        info("Insert query: $query")
        executeInsertOrThrow(query)
    }

    protected fun update(table: String, values: MapAttributes, where: String): Long {
        val setValues =
            values.entries.joinToString(", ") { (key, value) -> "$key = ${value?.prepareSQLValueAsString()}" }
        val query = "UPDATE $table SET $setValues WHERE $where;"
        info("Update query: $query")
        return executeUpdate(query)
    }

    protected fun delete(table: String, where: String): Long {
        val query = "DELETE FROM $table WHERE $where;"
        info("Delete query: $query")
        return executeDelete(query)
    }

    private fun executeInsertOrThrow(query: String) {
        if (driver.execute(null, query, 0).value == 0L) {
            throw IllegalStateException("Insertion failed")
        }
    }

    private fun buildCursorValues(tableName: String, cursor: SqlCursor): List<Cursor> {

        val columns = getColumns(tableName)
        val cursors = mutableListOf<Cursor>()
        var index = 0
        while (cursor.next().value) {

            val cursorValues = mutableListOf<CursorValue<*>>()

            columns.forEach { column ->
                cursorValues.add(
                    when (column.type) {
                        "INTEGER" -> CursorValue(cursor.getRequireInt(column.position), column)
                        "TEXT" -> CursorValue(cursor.getRequireString(column.position), column)
                        "REAL" -> CursorValue(cursor.getRequireDouble(column.position), column)
                        else -> throw IllegalArgumentException("Invalid column type")
                    }
                )
            }
            cursors.add(Cursor(index, tableName, cursorValues))
            index++
        }
        return cursors
    }

    private fun getTableName(statement: String): String {
        val regex = "SELECT .+ FROM (\\w+)".toRegex()
        val matchResult = regex.find(statement)
        val (tableName) = matchResult?.destructured
            ?: throw IllegalArgumentException("Invalid statement")
        return tableName
    }

    private fun executeUpdate(query: String): Long {
        return driver.execute(null, query, 0).value
    }

    private fun executeDelete(query: String): Long {
        return driver.execute(null, query, 0).value
    }


    /**
     * Builds a SQL WHERE clause from conditions and an operator.
     *
     * @param conditions The list of conditions.
     * @param operator The logical operator to combine conditions (AND/OR).
     * @return A pair consisting of the WHERE clause and the list of arguments.
     */
    protected fun buildWhereEvaluation(
        conditions: List<WhereCondition>,
        operator: Operator
    ): String {
        return conditions.joinToString(
            operator.name,
            transform = { " ${it.columnValue.column} ${it.comparator} ${it.columnValue.value.prepareSQLValueAsString()} " }
        ).trim()
    }

    protected fun List<DBColumnValue>.prepareMap(): MapAttributes {
        return associate { it.column to it.value }
    }

    companion object {
        private val TABLES_SYSTEM = listOf("android_metadata", "sqlite_sequence")
        private var CACHE_TABLES = mutableMapOf<String, List<String>>()
        private var CACHE_COLUMN_NAMES =
            mutableMapOf<String, MutableMap<String, List<Column>>>()

        fun flushCache() {
            CACHE_TABLES = mutableMapOf()
            CACHE_COLUMN_NAMES = mutableMapOf()
        }
    }


}