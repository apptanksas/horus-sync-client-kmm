package com.apptank.horus.client.extensions

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
internal fun Any?.prepareSQLValueAsString(): String {

    if (this == null) return "NULL"

    return when (val value = this) {
        is String -> "'$value'"
        is Boolean -> if (value) BOOL_TRUE else BOOL_FALSE
        else -> value.toString()
    }
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



