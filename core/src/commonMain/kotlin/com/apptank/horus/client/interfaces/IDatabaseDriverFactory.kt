package com.apptank.horus.client.interfaces

import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.database.HorusDatabase


interface IDatabaseDriverFactory {
    fun getDatabaseName(): String
    fun getDatabase(): HorusDatabase
    fun getSchema(): HorusDatabase.Schema
    fun createDriver(): SqlDriver
}

