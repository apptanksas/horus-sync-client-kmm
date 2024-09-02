package com.apptank.horus.client

import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.extensions.prepareSQLValueAsString
import kotlinx.datetime.Clock
import java.util.UUID
import kotlin.random.Random

abstract class TestCase {
    protected fun SqlDriver.insertOrThrow(table: String, values: Map<String, Any>) {
        val columns = values.keys.joinToString(", ")
        val valuesString = values.values.joinToString(", ") { it.prepareSQLValueAsString() }
        val query = "INSERT INTO $table ($columns) VALUES ($valuesString);"

        if (execute(null, query, 0).value == 0L) {
            throw IllegalStateException("Insertion failed")
        }
    }

    protected fun SqlDriver.createTable(table: String, columns: Map<String, String>) {
        val columnsString = columns.entries.joinToString(", ") { (name, type) -> "$name $type" }
        val query = "CREATE TABLE $table ($columnsString);"
        execute(null, query, 0)
    }

    protected fun <T> generateArray(size: Int = 10, creator: () -> T): List<T> {
        val sizeList = Random.nextInt(1, size)
        return List(sizeList) { Random.nextInt(0, size) }.map {
            creator()
        }
    }

    protected fun uuid(): String {
        return UUID.randomUUID().toString()
    }

    protected fun timestamp(): Long {
        return Clock.System.now().toEpochMilliseconds() / 1000
    }
}