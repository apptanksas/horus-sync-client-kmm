package com.apptank.horus.client.di

import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.migration.network.service.IMigrationService
import com.apptank.horus.client.migration.network.service.MigrationService
import com.russhwolf.settings.Settings
import io.ktor.client.engine.cio.CIO


object HorusContainer {

    private var settings: Settings? = null
    private var databaseFactory: IDatabaseDriverFactory? = null
    private var baseUrl: String? = null

    private val httpClient by lazy {
        CIO.create {
            requestTimeout = 60 * 1000
        }
    }

    private var migrationService: IMigrationService? = null

    // ------------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------------


    fun setupMigrationService(service: IMigrationService) {
        migrationService = service
    }

    fun setupDatabaseFactory(factory: IDatabaseDriverFactory) {
        databaseFactory = factory
    }

    fun setupBaseUrl(url: String) {
        baseUrl = url
    }

    fun setupSettings(settings: Settings) {
        this.settings = settings
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    internal fun getMigrationService(): IMigrationService {
        return migrationService ?: MigrationService(httpClient, baseUrl!!)
    }

    internal fun getDatabaseFactory(): IDatabaseDriverFactory {
        return databaseFactory ?: throw IllegalStateException("Database factory not set")
    }

    internal fun getSettings(): Settings {
        return settings ?: throw IllegalStateException("Settings not set")
    }


}