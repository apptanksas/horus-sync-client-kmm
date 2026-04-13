package org.apptank.horus.client.sync.tasks

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.apptank.horus.client.DATA_MIGRATION_VERSION_3
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.buildEntitiesSchemeFromJSON
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.di.IDatabaseDriverFactory
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.migration.network.service.IMigrationService
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.apptank.horus.client.tasks.ControlTaskManager
import org.apptank.horus.client.tasks.ValidateMigrationLocalDatabaseTask
import com.russhwolf.settings.Settings
import dev.mokkery.answering.returns
import dev.mokkery.answering.sequentiallyReturns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matches
import dev.mokkery.MockMode
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import okio.Path.Companion.toPath
import org.apptank.horus.client.HorusDataFacade
import org.apptank.horus.client.MOCK_RESPONSE_GET_SYNC_STATUS
import org.apptank.horus.client.buildSyncDataStatusFromJSON
import org.apptank.horus.client.bus.InternalEventBus
import org.apptank.horus.client.bus.EventType
import org.apptank.horus.client.bus.HorusClientSyncErrorEventBus
import org.apptank.horus.client.bus.SyncError
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.serialization.AnySerializer
import org.apptank.horus.client.tasks.RefreshReadableEntitiesTask
import org.apptank.horus.client.tasks.RetrieveDataSharedTask
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.fail


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ControlTaskManagerTest : TestCase() {

    private lateinit var driver: JdbcSqliteDriver

    val networkValidator = mock<INetworkValidator>(MockMode.autofill)
    val migrationService = mock<IMigrationService>(MockMode.autofill)
    val synchronizationService = mock<ISynchronizationService>(MockMode.autofill)
    val databaseDriverFactory = mock<IDatabaseDriverFactory>(MockMode.autofill)
    val syncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
    val storageSettings = mock<Settings>(MockMode.autofill)


    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val database = HorusDatabase("database.db", driver)

        every { databaseDriverFactory.getDatabaseName() } returns "database.db"
        every { databaseDriverFactory.getDriver() } returns driver
        every { databaseDriverFactory.getDatabase() } returns database
        every { databaseDriverFactory.getSchema() } returns HorusDatabase.Schema

        with(HorusContainer) {
            setupNetworkValidator(networkValidator)
            setupSettings(storageSettings)
            setupMigrationService(migrationService)
            setupSynchronizationService(synchronizationService)
            setupDatabaseFactory(databaseDriverFactory)
            setupConfig(getHorusConfigTest())
        }

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
    }

    @After
    fun tearDown() {
        HorusAuthentication.clearSession()
    }

    @Test
    fun `start execution complete successfully`() = runBlocking {
        // Given
        val entitiesScheme = buildEntitiesSchemeFromJSON(DATA_MIGRATION_VERSION_3)
        val entitiesData = listOf(
            SyncDTO.Response.Entity(
                "products",
                mapOf(
                    "id" to uuid(),
                    "sync_hash" to randomHash(),
                    "sync_owner_id" to uuid(),
                    "sync_created_at" to timestamp(),
                    "sync_updated_at" to timestamp(),
                    "mv_size" to "1",
                    "mv_variant" to "1",
                    "volume" to "kg",
                    "weight" to "kg",
                    "type" to "1",
                    "name" to "Product  1",
                    "destination" to "1",
                )
            )
        )
        val syncDataStatus = buildSyncDataStatusFromJSON(MOCK_RESPONSE_GET_SYNC_STATUS)
        val taskExecutionCountExpected = 7
        val filename = "sync_data_" + (Random.nextUInt()) + ".ndjson"
        val pathFile = getHorusConfigTest().uploadFilesConfig.baseStoragePath + filename
        val path = createFileInLocalStorage(pathFile, entitiesData.map { AnySerializer.decoderJSON.encodeToString(it) }.joinToString("\n")).toPath()

        InternalEventBus.register(EventType.ON_PROGRESS_SYNC) {
            val progress = (it.data?.get("progress") as Int)
            assert(progress <= 100) {
                "Progress $progress should not be less than or equal to 100"
            }
        }

        everySuspend { migrationService.getMigration() } returns DataResult.Success(entitiesScheme)
        every { storageSettings.getLongOrNull(ValidateMigrationLocalDatabaseTask.KEY_SCHEMA_VERSION) } returns null
        every { storageSettings.getLongOrNull(RetrieveDataSharedTask.KEY_LAST_DATE_DATA_SHARED) } returns null
        every { storageSettings.getLongOrNull(RefreshReadableEntitiesTask.KEY_LAST_DATE_READABLE_ENTITIES) } returns null

        everySuspend { synchronizationService.postValidateHashing(any()) } returns DataResult.Success(
            SyncDTO.Response.HashingValidation(randomHash(), randomHash(), true)
        )
        everySuspend { synchronizationService.getData(any()) } returns DataResult.Success(entitiesData)

        everySuspend { synchronizationService.postStartSync(any()) } returns DataResult.Success(Unit)
        everySuspend { synchronizationService.getSyncStatus(any()) } returns DataResult.Success(syncDataStatus)
        everySuspend {
            synchronizationService.downloadSyncData(
                matches { it == syncDataStatus.downloadUrl },
                matches { true })
        } returns DataResult.Success(path)

        everySuspend { synchronizationService.getDataShared() } returns DataResult.Success(entitiesData)

        every { networkValidator.isNetworkAvailable() } sequentiallyReturns listOf(true, true, false, true, true, false)

        var isCompleted = false

        with(ControlTaskManager) {
            setOnCompleted {
                isCompleted = true
            }
            setOnCallbackStatusListener {
                if (it === ControlTaskManager.Status.FAILED) {
                    fail()
                }
            }
        }

        // When
        ControlTaskManager.start(Dispatchers.Default)
        delay(1000)

        // Then
        Assert.assertEquals(
            taskExecutionCountExpected,
            ControlTaskManager.getTaskExecutionCounter()
        )
        Assert.assertTrue(isCompleted)
    }

    @Test
    fun `start execution is failure by network is not available`() = runBlocking {
        // Given

        HorusContainer.setupSyncControlDatabaseHelper(syncControlDatabaseHelper)
        HorusContainer.setupNetworkValidator(networkValidator)

        every { networkValidator.isNetworkAvailable() } returns false
        every { syncControlDatabaseHelper.getEntityNames() } returns emptyList()

        var isFailed = false
        var eventBusCalled = false

        with(ControlTaskManager) {
            setOnCallbackStatusListener {
                if (it === ControlTaskManager.Status.FAILED) {
                    isFailed = true
                }
            }
        }

        HorusDataFacade.init()

        HorusClientSyncErrorEventBus.register {
            eventBusCalled = true
            assert(it is SyncError.NetworkError)
        }

        // When
        ControlTaskManager.start(Dispatchers.Default)
        delay(500)

        // Then
        Assert.assertTrue(isFailed)
        Assert.assertTrue(eventBusCalled)
    }

}