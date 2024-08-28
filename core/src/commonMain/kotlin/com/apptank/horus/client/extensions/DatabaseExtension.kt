package com.apptank.horus.client.extensions

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.config.BOOL_FALSE
import com.apptank.horus.client.config.BOOL_TRUE


/**
 * Prepares a value as a string for SQL operations.
 *
 * @param value The value to be prepared.
 * @return The string representation of the value.
 */
internal fun Any.prepareSQLValueAsString(): String {
    return when (val value = this) {
        is String -> "\"$value\""
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

fun <T> SqlDriver.rawQuery(query: String, mapper: (SqlCursor) -> T?): List<T> {
    return executeQuery(null, query, {
        val resultList = mutableListOf<T>()
        while (it.next().value) {
            mapper(it)?.let { item ->
                resultList.add(item)
            }
        }
        QueryResult.Value(resultList)
    }, 0).value
}

fun SqlDriver.use(block: (SqlDriver) -> Unit) {
    block(this)
}




