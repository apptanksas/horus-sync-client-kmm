package com.apptank.horus.client.di

import com.apptank.horus.client.interfaces.IDatabaseDriverFactory

object HorusContainer {

    internal var databaseFactory: IDatabaseDriverFactory? = null

    fun setupDatabaseFactory(factory: IDatabaseDriverFactory) {
        databaseFactory = factory
    }
}