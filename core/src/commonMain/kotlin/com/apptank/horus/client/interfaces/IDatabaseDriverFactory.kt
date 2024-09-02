package com.apptank.horus.client.interfaces

import app.cash.sqldelight.db.SqlDriver
import horus.HorusDatabase


interface IDatabaseDriverFactory {
    fun getDatabaseName(): String

    fun retrieveDatabase(): HorusDatabase
    fun createDriver(): SqlDriver
}

