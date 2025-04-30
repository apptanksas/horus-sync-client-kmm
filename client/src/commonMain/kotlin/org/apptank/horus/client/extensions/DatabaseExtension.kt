package org.apptank.horus.client.extensions

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.config.BOOL_FALSE
import org.apptank.horus.client.config.BOOL_TRUE


/**
 * Prepares the SQL representation of a value as a string.
 *
 * Converts various types of values into their appropriate SQL string representations:
 * - Strings are enclosed in single quotes.
 * - Booleans are converted to their corresponding SQL boolean literals.
 * - Other types are converted to their string representation.
 *
 * @return The SQL string representation of the value, or "NULL" if the value is null.
 */
internal fun Any?.prepareSQLValueAsString(): String {
    if (this == null) return "NULL"

    val sanitize: (String) -> String = { value ->
        value.replace("'", "''")
    }

    return when (val value = this) {
        is String -> "'${sanitize(value)}'"
        is CharSequence -> "'${sanitize(value.toString())}'"
        is Boolean -> if (value) BOOL_TRUE else BOOL_FALSE
        is List<*> -> "(${value.joinToString(",") { it.prepareSQLValueAsString() }})"
        is LocalDate -> value.atTime(12, 0, 0).toInstant(TimeZone.UTC).epochSeconds.toString()
        is LocalDateTime -> value.toInstant(TimeZone.UTC).epochSeconds.toString()
        is Instant -> value.epochSeconds.toString()
        else -> value.toString()
    }
}


/**
 * Creates an SQL INSERT statement for the specified table and values.
 *
 * @param table The name of the table to insert the values into.
 * @param values A map of column names to values to be inserted.
 * @return The SQL INSERT statement.
 */
internal fun SqlDriver.createSQLInsert(table: String, values: DataMap): String {
    val columns = values.keys.joinToString(", ")
    val valuesString = values.values.joinToString(", ") { it.prepareSQLValueAsString() }
    print("INSERT INTO $table ($columns) VALUES ($valuesString)")
    return "INSERT INTO $table ($columns) VALUES ($valuesString)"
}

/**
 * Executes a SQL query on the driver.
 *
 * Executes the provided SQL query with no additional parameters or special handling.
 *
 * @param query The SQL query to be executed.
 */
fun SqlDriver.execute(query: String) {
    execute(null, query, 0)
}

/**
 * Executes a block of code within the context of the `SqlDriver`.
 *
 * This inline function allows you to perform operations on the `SqlDriver` instance
 * using a lambda block.
 *
 * @param block The block of code to be executed.
 * @return The result of the block.
 */
internal inline fun <R> SqlDriver.handle(block: SqlDriver.() -> R): R {
    return kotlin.runCatching {
        block(this)
    }.getOrElse {
        logException("Error SQLDriver: ${it.message}", it)
        throw it
    }
}

/**
 * Retrieves an integer value from the `SqlCursor` at the specified index.
 *
 * Throws an `IllegalStateException` if the value at the specified index is null.
 *
 * @param index The index of the value to retrieve.
 * @return The integer value at the specified index.
 * @throws IllegalStateException If the value is null.
 */
fun SqlCursor.getRequireInt(index: Int): Int {
    return this.getLong(index)?.toInt() ?: throw IllegalStateException("Index $index not found")
}

/**
 * Retrieves a long value from the `SqlCursor` at the specified index.
 *
 * Throws an `IllegalStateException` if the value at the specified index is null.
 *
 * @param index The index of the value to retrieve.
 * @return The long value at the specified index.
 * @throws IllegalStateException If the value is null.
 */
fun SqlCursor.getRequireLong(index: Int): Long {
    return this.getLong(index) ?: throw IllegalStateException("Index $index not found")
}

/**
 * Retrieves a string value from the `SqlCursor` at the specified index.
 *
 * Throws an `IllegalStateException` if the value at the specified index is null.
 *
 * @param index The index of the value to retrieve.
 * @return The string value at the specified index.
 * @throws IllegalStateException If the value is null.
 */
fun SqlCursor.getRequireString(index: Int): String {
    return this.getString(index) ?: throw IllegalStateException("Index $index not found")
}

/**
 * Retrieves a double value from the `SqlCursor` at the specified index.
 *
 * Throws an `IllegalStateException` if the value at the specified index is null.
 *
 * @param index The index of the value to retrieve.
 * @return The double value at the specified index.
 * @throws IllegalStateException If the value is null.
 */
fun SqlCursor.getRequireDouble(index: Int): Double {
    return this.getDouble(index) ?: throw IllegalStateException("Index $index not found")
}

/**
 * Retrieves a boolean value from the `SqlCursor` at the specified index.
 *
 * Throws an `IllegalStateException` if the value at the specified index is null.
 *
 * @param index The index of the value to retrieve.
 * @return The boolean value at the specified index.
 * @throws IllegalStateException If the value is null.
 */
fun SqlCursor.getRequireBoolean(index: Int): Boolean {
    return this.getBoolean(index) ?: throw IllegalStateException("Index $index not found")
}
