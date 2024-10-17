package org.apptank.horus.client.di

import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.database.SyncControlDatabaseHelper
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.database.OperationDatabaseHelper
import org.apptank.horus.client.migration.network.service.IMigrationService
import org.apptank.horus.client.migration.network.service.MigrationService
import org.apptank.horus.client.sync.manager.RemoteSynchronizatorManager
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.apptank.horus.client.sync.network.service.SynchronizationService
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.apptank.horus.client.config.HorusConfig
import org.apptank.horus.client.control.helper.ISyncFileDatabaseHelper
import org.apptank.horus.client.sync.manager.DispenserManager
import org.apptank.horus.client.sync.network.service.IFileSynchronizationService
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository
import org.apptank.horus.client.sync.upload.repository.UploadFileRepository


/**
 * Singleton object for managing application-wide services and configurations.
 *
 * This object handles the setup, retrieval, and clearing of various services and configurations required by the application.
 */
object HorusContainer {

    private var config: HorusConfig? = null

    private var settings: Settings? = null
    private var databaseFactory: IDatabaseDriverFactory? = null

    private val httpClient by lazy {
        HttpClient() {
            install(HttpTimeout) {
                requestTimeoutMillis = 60L * 1000 // 60 secs
            }
        }
    }

    private var migrationService: IMigrationService? = null

    private var synchronizationService: ISynchronizationService? = null

    private var fileSynchronizationService: IFileSynchronizationService? = null

    private var syncControlDatabaseHelper: ISyncControlDatabaseHelper? = null

    private var operationDatabaseHelper: IOperationDatabaseHelper? = null

    private var syncFilesDatabaseHelper: ISyncFileDatabaseHelper? = null

    private var uploadFileRepository: IUploadFileRepository? = null

    private var networkValidator: INetworkValidator? = null

    private var logger: ILogger? = null

    private var remoteSynchronizatorManager: RemoteSynchronizatorManager? = null

    private var dispenserManager: DispenserManager? = null

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
     * Sets up the file synchronization service.
     *
     * @param service The [IFileSynchronizationService] instance to set up.
     */
    internal fun setupFileSynchronizationService(service: IFileSynchronizationService) {
        fileSynchronizationService = service
    }

    /**
     * Sets up the remote synchronizator manager.
     *
     * @param manager The [RemoteSynchronizatorManager] instance to set up.
     */
    internal fun setupRemoteSynchronizatorManager(manager: RemoteSynchronizatorManager) {
        remoteSynchronizatorManager = manager
    }

    /**
     * Sets up the dispenser manager.
     *
     * @param manager The [DispenserManager] instance to set up.
     */
    internal fun setupDispenserManager(manager: DispenserManager) {
        dispenserManager = manager
    }

    /**
     * Sets up the sync control database helper.
     *
     * @param helper The [ISyncControlDatabaseHelper] instance to set up.
     */
    internal fun setupSyncControlDatabaseHelper(helper: ISyncControlDatabaseHelper) {
        syncControlDatabaseHelper = helper
    }

    /**
     * Sets up the operation database helper.
     *
     * @param helper The [IOperationDatabaseHelper] instance to set up.
     */
    internal fun setupOperationDatabaseHelper(helper: IOperationDatabaseHelper) {
        operationDatabaseHelper = helper
    }

    /**
     * Sets up the sync files database helper.
     *
     * @param helper The [ISyncFileDatabaseHelper] instance to set up.
     */
    internal fun setupSyncFilesDatabaseHelper(helper: ISyncFileDatabaseHelper) {
        syncFilesDatabaseHelper = helper
    }

    /**
     * Sets up the upload file repository.
     *
     * @param repository The [IUploadFileRepository] instance to set up.
     */
    internal fun setupUploadFileRepository(repository: IUploadFileRepository) {
        uploadFileRepository = repository
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
            factory.getDriver()
        )
        syncControlDatabaseHelper = SyncControlDatabaseHelper(
            factory.getDatabaseName(),
            factory.getDriver()
        )
    }

    /**
     * Sets up the base URL for network requests.
     *
     * @param url The base URL to set up.
     */
    fun setupConfig(config: HorusConfig) {
        this.config = config
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

    /**
     * Sets up the logger.
     *
     * @param logger The [ILogger] instance to set up.
     */
    fun setupLogger(logger: ILogger) {
        this.logger = logger
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
        return migrationService ?: MigrationService(httpClient.engine, getConfig().baseUrl)
    }

    /**
     * Retrieves the synchronization service.
     *
     * @return The [ISynchronizationService] instance.
     * @throws IllegalStateException if the synchronization service is not set.
     */
    internal fun getSynchronizationService(): ISynchronizationService {
        if (synchronizationService == null) {
            synchronizationService = SynchronizationService(httpClient.engine, getConfig().baseUrl)
        }
        return synchronizationService!!
    }

    /**
     * Retrieves the file synchronization service.
     *
     * @return The [IFileSynchronizationService] instance.
     * @throws IllegalStateException if the file synchronization service is not set.
     */
    internal fun getFileSynchronizationService(): IFileSynchronizationService {
        return fileSynchronizationService
            ?: throw IllegalStateException("FileSynchronizationService not set")
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

    /** Retrieves the configuration.
     *
     * @return The [HorusConfig] instance.
     * @throws IllegalStateException if the configuration is not set.
     */
    internal fun getConfig(): HorusConfig {
        return config ?: throw IllegalStateException("Config not set")
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

    internal fun getSyncFilesDatabaseHelper(): ISyncFileDatabaseHelper {
        return syncFilesDatabaseHelper
            ?: throw IllegalStateException("SyncFilesDatabaseHelper not set")
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

    /**
     * Retrieves the logger.
     *
     * @return The [ILogger] instance.
     */
    internal fun getLogger(): ILogger? {
        return logger
    }

    /**
     * Retrieves the remote synchronizator manager.
     *
     * @return A new instance of [RemoteSynchronizatorManager].
     */
    internal fun getRemoteSynchronizatorManager(): RemoteSynchronizatorManager {
        if (remoteSynchronizatorManager == null) {
            remoteSynchronizatorManager = RemoteSynchronizatorManager(
                getNetworkValidator(),
                getSyncControlDatabaseHelper(),
                getSynchronizationService()
            )
        }
        return remoteSynchronizatorManager!!
    }

    /**
     * Retrieves the dispenser manager.
     *
     * @return A new instance of [DispenserManager].
     */
    internal fun getDispenserManager(): DispenserManager {
        if (dispenserManager == null) {
            dispenserManager = DispenserManager(
                getConfig().pushPendingActionsConfig.batchSize,
                getConfig().pushPendingActionsConfig.expirationTime,
                getSyncControlDatabaseHelper(),
                getRemoteSynchronizatorManager()
            )
        }
        return dispenserManager!!
    }

    /**
     * Retrieves the upload file repository.
     *
     * @return A new instance of [IUploadFileRepository].
     */
    internal fun getUploadFileRepository(): IUploadFileRepository {
        if (uploadFileRepository == null) {
            uploadFileRepository = UploadFileRepository(
                getConfig(),
                getSyncFilesDatabaseHelper(),
                getFileSynchronizationService()
            )
        }
        return uploadFileRepository!!
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
        config = null
        migrationService = null
        synchronizationService = null
        syncControlDatabaseHelper = null
        operationDatabaseHelper = null
        networkValidator = null
    }
}
