package com.apptank.horus.client.extensions

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.config.BOOL_FALSE
import com.apptank.horus.client.config.BOOL_TRUE
import kotlinx.serialization.json.Json


/**
 * Prepares a value as a string for SQL operations.
 *
 * @param value The value to be prepared.
 * @return The string representation of the value.
 */
internal fun Any.prepareSQLValueAsString(): String {
    return when (val value = this) {
        is String -> "'$value'"
        is Boolean -> if (value) BOOL_TRUE else BOOL_FALSE
        else -> value.toString()
    }
}

/**
 * Retrieves the value of the specified attribute from the cursor.
 * The return type is determined based on the attribute's type in the cursor.
 *
 * @param attribute the name of the attribute to retrieve.
 * @return the value of the specified attribute, cast to the appropriate type.
 * @throws IllegalStateException if the attribute type is unknown.
 */
fun <T : Any> SqlCursor.getValue(attribute: String): T {
    TODO("Not yet implemented")
    /* return when (getType(getIndex(attribute))) {
         Cursor.FIELD_TYPE_STRING -> getString(getIndex(attribute)) as T
         Cursor.FIELD_TYPE_INTEGER -> getLong(getIndex(attribute)) as T
         Cursor.FIELD_TYPE_FLOAT -> getDouble(getIndex(attribute)) as T
         else -> throw IllegalStateException("Attribute [$attribute] error field!")
     } */
}

/**
 * Retrieves the index of the specified column name from the cursor.
 *
 * @param column the name of the column to retrieve the index for.
 * @return the index of the specified column.
 * @throws IllegalStateException if the column does not exist in the cursor.
 */
fun SqlCursor.getIndex(column: String): Int {
    TODO("Not yet implemented")
    /*
    val index = getColumnIndex(column)
    if (index < 0)
        throw IllegalStateException("Index [$column] does not exist")
    return index*/
}

fun SqlDriver.execute(query: String) {
    execute(null, query, 0)
}


inline fun <R> SqlDriver.handle(block: SqlDriver.() -> R): R {
    return block(this)
}

fun SqlCursor.getRequireInt(index: Int): Int {
    return this.getLong(index)?.toInt() ?: throw IllegalStateException("Index $index not found")
}

fun SqlCursor.getRequireLong(index: Int): Long {
    return this.getLong(index) ?: throw IllegalStateException("Index $index not found")
}

fun SqlCursor.getRequireString(index: Int): String {
    return this.getString(index) ?: throw IllegalStateException("Index $index not found")
}

fun SqlCursor.getRequireDouble(index: Int): Double {
    return this.getDouble(index) ?: throw IllegalStateException("Index $index not found")
}

fun SqlCursor.getRequireBoolean(index: Int): Boolean {
    return this.getBoolean(index) ?: throw IllegalStateException("Index $index not found")
}

private val decoder = Json { ignoreUnknownKeys = true }
fun SqlCursor.getStringAndConvertToMap(attributeName: String): Map<String, Any> {
    return decoder.decodeFromString<Map<String, Any>>(this.getValue(attributeName))
}


