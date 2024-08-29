package com.apptank.horus.client

import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.extensions.prepareSQLValueAsString

abstract class TestCase {
    protected fun SqlDriver.insertOrThrow(table: String, values: Map<String, Any>) {
        val columns = values.keys.joinToString(", ")
        val valuesString = values.values.joinToString(", ") { it.prepareSQLValueAsString() }
        val query = "INSERT INTO $table ($columns) VALUES ($valuesString);"

        if (execute(null, query, 0).value == 0L) {
            throw IllegalStateException("Insertion failed")
        }
    }
}