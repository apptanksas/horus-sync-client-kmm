package com.apptank.horus.client.di

import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncControlDatabaseHelper
import com.apptank.horus.client.database.IOperationDatabaseHelper
import com.apptank.horus.client.database.OperationDatabaseHelper
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.migration.network.service.IMigrationService
import com.apptank.horus.client.migration.network.service.MigrationService
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import com.apptank.horus.client.sync.network.service.SynchronizationService
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

    private var synchronizationService: ISynchronizationService? = null

    private var syncControlDatabaseHelper: ISyncControlDatabaseHelper? = null

    private var operationDatabaseHelper: IOperationDatabaseHelper? = null

    private var networkValidator: INetworkValidator? = null

    // ------------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------------


    internal fun setupMigrationService(service: IMigrationService) {
        migrationService = service
    }

    internal fun setupSynchronizationService(service: ISynchronizationService) {
        synchronizationService = service
    }

    fun setupDatabaseFactory(factory: IDatabaseDriverFactory) {
        databaseFactory = factory
        operationDatabaseHelper = OperationDatabaseHelper(
            factory.getDatabaseName(),
            factory.createDriver()
        )
        syncControlDatabaseHelper = SyncControlDatabaseHelper(
            factory.getDatabaseName(),
            factory.createDriver()
        )
    }

    fun setupBaseUrl(url: String) {
        baseUrl = url
    }

    fun setupSettings(settings: Settings) {
        this.settings = settings
    }

    fun setupNetworkValidator(networkValidator: INetworkValidator) {
        this.networkValidator = networkValidator
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    internal fun getMigrationService(): IMigrationService {
        return migrationService ?: MigrationService(httpClient, baseUrl!!)
    }

    internal fun getSynchronizationService(): ISynchronizationService {
        return synchronizationService ?: SynchronizationService(httpClient, baseUrl!!)
    }

    internal fun getDatabaseFactory(): IDatabaseDriverFactory {
        return databaseFactory ?: throw IllegalStateException("Database factory not set")
    }

    internal fun getSettings(): Settings {
        return settings ?: throw IllegalStateException("Settings not set")
    }

    internal fun getSyncControlDatabaseHelper(): ISyncControlDatabaseHelper {
        return syncControlDatabaseHelper
            ?: throw IllegalStateException("SyncControlDatabaseHelper not set")
    }

    internal fun getOperationDatabaseHelper(): IOperationDatabaseHelper {
        return operationDatabaseHelper
            ?: throw IllegalStateException("OperationDatabaseHelper not set")
    }

    internal fun getNetworkValidator(): INetworkValidator {
        return networkValidator
            ?: throw IllegalStateException("NetworkValidator not set")
    }

}