package org.apptank.horus.client.database

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.control.EntitiesTable
import org.apptank.horus.client.data.InternalModel
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.extensions.createSQLInsert
import org.apptank.horus.client.extensions.getRequireBoolean
import org.apptank.horus.client.extensions.getRequireDouble
import org.apptank.horus.client.extensions.getRequireInt
import org.apptank.horus.client.extensions.getRequireString
import org.apptank.horus.client.extensions.prepareSQLValueAsString
import org.apptank.horus.client.extensions.handle
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.notContains
import kotlin.random.Random

/**
 * Abstract class that provides helper methods for interacting with an SQLite database.
 *
 * @property driver The SQL driver used to execute queries.
 * @property databaseName The name of the database.
 */
abstract class SQLiteHelper(
    driver: SqlDriver,
    private val databaseName: String
) : TransacterImpl(driver) {

    /**
     * Retrieves the names of all tables in the database.
     *
     * @return A list of table names.
     */
    internal fun getTableEntities(): List<InternalModel.TableEntity> {

        if (CACHE_TABLES[databaseName]?.isNotEmpty() == true) {
            return CACHE_TABLES[databaseName] ?: emptyList()
        }

        val tables = mutableListOf<InternalModel.TableEntity>()

        val simpleQuery = SimpleQueryBuilder(EntitiesTable.TABLE_NAME)
            .build()

        this.driver.handle {
            // Query tables
            val result: List<InternalModel.TableEntity> = queryResult(simpleQuery) { cursor ->
                InternalModel.TableEntity(
                    cursor.getValue(EntitiesTable.ATTR_NAME),
                    cursor.getValue(EntitiesTable.ATTR_IS_WRITABLE)
                )
            }

            tables.addAll(result)
        }

        return tables.also {
            CACHE_TABLES[databaseName] = it
        }
    }

    /**
     * Retrieves the names of all tables in the database.
     *
     * @return A list of table names.
     */
    fun getTables(): List<String> {
        this.driver.handle {
            // Query tables
            val result: List<String> = rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'"
            ) { cursor -> cursor.getString(0) }

            return (result.filter { TABLES_SYSTEM.notContains(it) })
        }
    }

    /**
     * Retrieves the columns of a specified table.
     *
     * @param tableName The name of the table.
     * @return A list of columns in the table.
     */
    internal fun getColumns(tableName: String): List<Column> {

        if (CACHE_COLUMN_NAMES[databaseName]?.contains(tableName) == true) {
            return CACHE_COLUMN_NAMES[databaseName]?.get(tableName) ?: emptyList()
        }
        val query = "PRAGMA table_info($tableName);" // Query columns

        val columns = driver.handle {
            rawQuery(query) { cursor ->
                Column(
                    cursor.getRequireInt(0),
                    cursor.getRequireString(1),
                    cursor.getRequireString(2),
                    cursor.getRequireBoolean(3).not(),
                )
            }
        }

        return columns.also {
            if (CACHE_COLUMN_NAMES[databaseName] == null)
                CACHE_COLUMN_NAMES[databaseName] = mutableMapOf()
            CACHE_COLUMN_NAMES[databaseName]?.set(tableName, it)
        }
    }

    /**
     * Executes a raw SQL query and maps the result using the provided mapper function.
     *
     * @param query The SQL query to execute.
     * @param mapper A function that maps the cursor to a result of type [T].
     * @return A list of mapped results.
     */
    protected fun <T> rawQuery(query: String, mapper: (SqlCursor) -> T?): List<T> {
        return transactionWithResult {
            driver.executeQuery(Random.nextInt(), query, {
                val resultList = mutableListOf<T>()
                while (it.next().value) {
                    mapper(it)?.let { item ->
                        resultList.add(item)
                    }
                }
                QueryResult.Value(resultList)
            }, 0).value
        }
    }

    /**
     * Executes a query and maps the result to a list of [Cursor] objects using the provided mapper function.
     *
     * @param query The SQL query to execute.
     * @param mapper A function that maps the cursor to a result of type [T].
     * @return A list of mapped results.
     */
    internal fun <T> queryResult(query: String, mapper: (Cursor) -> T?): List<T> {
        return transactionWithResult {
            val tableName = getTableName(query)
            val attributes = extractSelectAttributes(query)

            driver.executeQuery(null, query, {
                val resultList = mutableListOf<T>()
                buildCursorValues(tableName, attributes, it).forEach { cursor ->
                    mapper(cursor)?.let { item ->
                        resultList.add(item)
                    }
                }
                QueryResult.Value(resultList)
            }, 0).value
        }
    }

    /**
     * Inserts data into a specified table, throwing an exception if the insertion fails.
     *
     * @param table The name of the table.
     * @param values The data to insert.
     */
    protected fun insertOrThrow(table: String, values: DataMap) {
        val query = driver.createSQLInsert(table, values)
        info("Insert query: $query")
        executeInsertOrThrow(query)
    }

    /**
     * Updates records in a specified table with given values and conditions.
     *
     * @param table The name of the table.
     * @param values The values to update.
     * @param where The SQL WHERE clause.
     * @return The number of rows affected.
     */
    protected fun update(table: String, values: DataMap, where: String): Long {
        val setValues =
            values.entries.joinToString(", ") { (key, value) -> "$key = ${value?.prepareSQLValueAsString()}" }
        val query = "UPDATE $table SET $setValues WHERE $where;"
        info("Update query: $query")
        return executeUpdate(query)
    }

    /**
     * Deletes records from a specified table based on conditions.
     *
     * @param table The name of the table.
     * @param where The SQL WHERE clause.
     * @return The number of rows affected.
     */
    protected fun delete(table: String, where: String): Long {
        val query = "DELETE FROM $table WHERE $where;"
        info("Delete query: $query")
        return executeDelete(query)
    }

    private fun executeInsertOrThrow(query: String) {
        driver.handle {
            if (execute(null, query, 0).value == 0L) {
                throw IllegalStateException("Insertion failed")
            }
        }
    }

    private fun buildCursorValues(
        tableName: String,
        attributesSelected: List<String>,
        cursor: SqlCursor
    ): List<Cursor> {

        val columns = filtrateColumns(getColumns(tableName), attributesSelected)

        val cursors = mutableListOf<Cursor>()
        var index = 0

        while (cursor.next().value) {
            val cursorValues = mutableListOf<CursorValue<*>>()
            columns.forEach { column ->
                cursorValues.add(
                    when (column.type) {
                        "INTEGER" -> CursorValue(cursor.getRequireInt(column.position), column)
                        "STRING" -> CursorValue(cursor.getRequireString(column.position), column)
                        "TEXT" -> CursorValue(cursor.getRequireString(column.position), column)
                        "REAL" -> CursorValue(cursor.getRequireDouble(column.position), column)
                        "BOOLEAN" -> CursorValue(cursor.getRequireBoolean(column.position), column)
                        "FLOAT" -> CursorValue(
                            cursor.getRequireDouble(column.position).toFloat(),
                            column
                        )

                        else -> throw IllegalArgumentException("Invalid column type ${column.type}")
                    }
                )
            }
            cursors.add(Cursor(index, tableName, cursorValues))
            index++
        }
        return cursors
    }

    private fun filtrateColumns(
        columns: List<Column>,
        attributesSelected: List<String>
    ): List<Column> {
        return columns.filter { column ->
            (attributesSelected.isNotEmpty() && attributesSelected.contains(column.name)) || attributesSelected.isEmpty()
        }.mapIndexed { index, column -> Column(index, column.name, column.type, column.nullable) }
    }

    /**
     * Retrieves the table name from a SQL SELECT statement.
     *
     * @param statement The SQL SELECT statement.
     * @return The name of the table.
     */
    private fun getTableName(statement: String): String {
        val regex = "SELECT .+ FROM (\\w+)".toRegex()
        val matchResult = regex.find(statement)
        val (tableName) = matchResult?.destructured
            ?: throw IllegalArgumentException("Invalid statement")
        return tableName
    }


    /**
     * Extracts the attributes selected in a SQL SELECT statement.
     *
     * @param statement The SQL SELECT statement.
     * @return A list of selected attributes.
     */
    private fun extractSelectAttributes(statement: String): List<String> {
        val regex = "SELECT (.+) FROM \\w+".toRegex()
        val matchResult = regex.find(statement)
        val (attributes) = matchResult?.destructured
            ?: throw IllegalArgumentException("Invalid statement")
        if (attributes == "*") return emptyList()

        if (attributes.contains(", "))
            return attributes.split(", ")

        return attributes.split(",")
    }

    private fun executeUpdate(query: String): Long {
        return driver.execute(null, query, 0).value
    }

    private fun executeDelete(query: String): Long {
        return driver.execute(null, query, 0).value
    }

    /**
     * Builds a SQL WHERE clause from conditions and a logical operator.
     *
     * @param conditions The list of conditions.
     * @param operator The logical operator to combine conditions (AND/OR).
     * @return The SQL WHERE clause.
     */
    protected fun buildWhereEvaluation(
        conditions: List<SQL.WhereCondition>,
        operator: SQL.LogicOperator
    ): String {
        return conditions.joinToString(
            operator.name,
            transform = { " ${it.columnValue.column} ${it.comparator.value} ${it.columnValue.value.prepareSQLValueAsString()} " }
        ).trim()
    }

    /**
     * Converts a list of [SQL.ColumnValue] to a [DataMap].
     *
     * @return A map of column names to values.
     */
    protected fun List<SQL.ColumnValue>.prepareMap(): DataMap {
        return associate { it.column to it.value }
    }

    companion object {
        private val TABLES_SYSTEM = listOf("android_metadata", "sqlite_sequence")
        private var CACHE_TABLES = mutableMapOf<String, List<InternalModel.TableEntity>>()
        private var CACHE_COLUMN_NAMES =
            mutableMapOf<String, MutableMap<String, List<Column>>>()

        /**
         * Clears the cached tables and column names.
         */
        fun flushCache() {
            CACHE_TABLES = mutableMapOf()
            CACHE_COLUMN_NAMES = mutableMapOf()
        }
    }
}