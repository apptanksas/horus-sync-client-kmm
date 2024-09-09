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


/**
 * Singleton object for managing application-wide services and configurations.
 *
 * This object handles the setup, retrieval, and clearing of various services and configurations required by the application.
 */
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

    /**
     * Sets up the migration service.
     *
     * @param service The [IMigrationService] instance to set up.
     */
    internal fun setupMigrationService(service: IMigrationService) {
        migrationService = service
    }

    /**
     * Sets up the synchronization service.
     *
     * @param service The [ISynchronizationService] instance to set up.
     */
    internal fun setupSynchronizationService(service: ISynchronizationService) {
        synchronizationService = service
    }

    /**
     * Sets up the database factory and initializes database helpers.
     *
     * @param factory The [IDatabaseDriverFactory] instance to set up.
     */
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

    /**
     * Sets up the base URL for network requests.
     *
     * @param url The base URL to set up.
     */
    fun setupBaseUrl(url: String) {
        baseUrl = url
    }

    /**
     * Sets up application settings.
     *
     * @param settings The [Settings] instance to set up.
     */
    fun setupSettings(settings: Settings) {
        this.settings = settings
    }

    /**
     * Sets up the network validator.
     *
     * @param networkValidator The [INetworkValidator] instance to set up.
     */
    fun setupNetworkValidator(networkValidator: INetworkValidator) {
        this.networkValidator = networkValidator
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    /**
     * Retrieves the migration service.
     *
     * @return The [IMigrationService] instance.
     * @throws IllegalStateException if the migration service is not set.
     */
    internal fun getMigrationService(): IMigrationService {
        return migrationService ?: MigrationService(httpClient, baseUrl!!)
    }

    /**
     * Retrieves the synchronization service.
     *
     * @return The [ISynchronizationService] instance.
     * @throws IllegalStateException if the synchronization service is not set.
     */
    internal fun getSynchronizationService(): ISynchronizationService {
        return synchronizationService ?: SynchronizationService(httpClient, baseUrl!!)
    }

    /**
     * Retrieves the database factory.
     *
     * @return The [IDatabaseDriverFactory] instance.
     * @throws IllegalStateException if the database factory is not set.
     */
    internal fun getDatabaseFactory(): IDatabaseDriverFactory {
        return databaseFactory ?: throw IllegalStateException("Database factory not set")
    }

    /**
     * Retrieves the application settings.
     *
     * @return The [Settings] instance.
     * @throws IllegalStateException if the settings are not set.
     */
    internal fun getSettings(): Settings {
        return settings ?: throw IllegalStateException("Settings not set")
    }

    /**
     * Retrieves the sync control database helper.
     *
     * @return The [ISyncControlDatabaseHelper] instance.
     * @throws IllegalStateException if the sync control database helper is not set.
     */
    internal fun getSyncControlDatabaseHelper(): ISyncControlDatabaseHelper {
        return syncControlDatabaseHelper
            ?: throw IllegalStateException("SyncControlDatabaseHelper not set")
    }

    /**
     * Retrieves the operation database helper.
     *
     * @return The [IOperationDatabaseHelper] instance.
     * @throws IllegalStateException if the operation database helper is not set.
     */
    internal fun getOperationDatabaseHelper(): IOperationDatabaseHelper {
        return operationDatabaseHelper
            ?: throw IllegalStateException("OperationDatabaseHelper not set")
    }

    /**
     * Retrieves the network validator.
     *
     * @return The [INetworkValidator] instance.
     * @throws IllegalStateException if the network validator is not set.
     */
    internal fun getNetworkValidator(): INetworkValidator {
        return networkValidator
            ?: throw IllegalStateException("NetworkValidator not set")
    }

    // ------------------------------------------------------------------------
    // Clear
    // ------------------------------------------------------------------------

    /**
     * Clears all stored configurations and services.
     */
    internal fun clear() {
        settings = null
        databaseFactory = null
        baseUrl = null
        migrationService = null
        synchronizationService = null
        syncControlDatabaseHelper = null
        operationDatabaseHelper = null
        networkValidator = null
    }
}
