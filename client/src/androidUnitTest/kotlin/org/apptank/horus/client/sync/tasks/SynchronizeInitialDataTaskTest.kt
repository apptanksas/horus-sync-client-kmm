package org.apptank.horus.client.sync.tasks

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.apptank.horus.client.DATA_MIGRATION_INITIAL_DATA_TASK
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.buildEntitiesSchemeFromJSON
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.database.SyncControlDatabaseHelper
import org.apptank.horus.client.database.HorusDatabase
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.database.OperationDatabaseHelper
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.extensions.execute
import org.apptank.horus.client.migration.network.toScheme
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.apptank.horus.client.tasks.SynchronizeInitialDataTask
import org.apptank.horus.client.tasks.TaskResult
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.every
import io.mockative.matches
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.apptank.horus.client.MOCK_RESPONSE_GET_SYNC_STATUS
import org.apptank.horus.client.MOCK_RESPONSE_SYNC_DATA_FILE
import org.apptank.horus.client.buildSyncDataStatusFromJSON
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.random.nextUInt


class SynchronizeInitialDataTaskTest : TestCase() {

    @Mock
    private val operationDatabaseHelper = mock(classOf<IOperationDatabaseHelper>())

    @Mock
    private val syncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    private val synchronizeService = mock(classOf<ISynchronizationService>())

    @Mock
    private val networkValidator = mock(classOf<INetworkValidator>())

    private lateinit var task: SynchronizeInitialDataTask

    @Before
    fun setup() {
        task = SynchronizeInitialDataTask(
            networkValidator,
            operationDatabaseHelper,
            syncControlDatabaseHelper,
            synchronizeService,
            getMockValidateHashingTask()
        )
    }

    @Test
    fun `when initial synchronization is completed then return success`(): Unit = runBlocking {
        // Given
        every { syncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) }
            .returns(true)

        // When
        val result = task.execute(null, 0, 10)

        // Then
        assert(result is TaskResult.Success)
        coVerify { synchronizeService.getData(any()) }.wasNotInvoked()
    }

    @Test
    fun `when initial synchronization is not completed then synchronize data is failure`(): Unit =
        runBlocking {
            // Given
            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) }
                .returns(false)
            coEvery { synchronizeService.getData() }.returns(DataResult.Failure(Exception("Error synchronizing data")))

            // SynchronizationService mock responses
            coEvery { synchronizeService.postStartSync(any()) }.returns(DataResult.Failure(Exception("Error synchronizing data")))

            // When
            val result = task.execute(null, 0, 10)

            // Then
            assert(result is TaskResult.Failure)
        }

    @Test
    fun `when initial synchronization is not completed then synchronize data is success`(): Unit =
        runBlocking {
            // Given
            val syncDataStatus = buildSyncDataStatusFromJSON(MOCK_RESPONSE_GET_SYNC_STATUS)

            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) }.returns(false)

            every {
                operationDatabaseHelper.insertWithTransaction(
                    any(),
                    callbackMatcher()
                )
            }.returns(true)

            // SynchronizationService mock responses
            coEvery { synchronizeService.postStartSync(any()) }.returns(DataResult.Success(Unit))
            coEvery { synchronizeService.getSyncStatus(any()) }.returns(DataResult.Success(syncDataStatus))
            every { syncControlDatabaseHelper.getEntityLevel(any()) }.returns(1)

            mockDownloadSyncData(syncDataStatus.downloadUrl)

            // When
            val result = task.execute(null, 0, 10)

            // Then
            assert(result is TaskResult.Success)
            verify { operationDatabaseHelper.insertWithTransaction(any(), any()) }.wasInvoked()
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.INITIAL_SYNCHRONIZATION,
                    SyncControl.Status.COMPLETED
                )
            }.wasInvoked()
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.COMPLETED
                )
            }.wasInvoked()
        }

    @Test
    fun `when network is not available then return failure`(): Unit = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(false)
        every { syncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) }
            .returns(false)

        // When
        val result = task.execute(null, 0, 10)

        // Then
        assert(result is TaskResult.Failure)
        coVerify { synchronizeService.getData(any()) }.wasNotInvoked()
    }

    @Test
    fun `when get data is success then migration success with database constraints`(): Unit = runBlocking {

        // Given
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute("PRAGMA foreign_keys=ON")

        val operationDatabaseHelper = OperationDatabaseHelper("database", driver)
        val syncControlDatabaseHelper = SyncControlDatabaseHelper("database", driver)

        val task = SynchronizeInitialDataTask(
            networkValidator,
            operationDatabaseHelper,
            syncControlDatabaseHelper,
            synchronizeService,
            getMockValidateHashingTask()
        )

        val entitiesScheme = buildEntitiesSchemeFromJSON(DATA_MIGRATION_INITIAL_DATA_TASK).map { it.toScheme() }
        val syncDataStatus = buildSyncDataStatusFromJSON(MOCK_RESPONSE_GET_SYNC_STATUS)

        every { networkValidator.isNetworkAvailable() }.returns(true)
        mockDownloadSyncData(syncDataStatus.downloadUrl)

        // SynchronizationService mock responses
        coEvery { synchronizeService.postStartSync(any()) }.returns(DataResult.Success(Unit))
        coEvery { synchronizeService.getSyncStatus(any()) }.returns(DataResult.Success(syncDataStatus))

        HorusDatabase.Schema.create(driver, entitiesScheme)

        // When
        val result = task.execute(null, 0, 10)

        // Then
        assert(result is TaskResult.Success)
    }


    private suspend fun mockDownloadSyncData(url: String?) {
        val filename = "sync_data_" + (Random.nextUInt()) + ".ndjson"
        val pathFile = getLocalTestPath(filename)
        val path = createFileInLocalStorage(pathFile, MOCK_RESPONSE_SYNC_DATA_FILE).toPath()

        coEvery { synchronizeService.downloadSyncData(matches { it == url }, matches { true }) }.returns(
            DataResult.Success(
                path
            )
        )

    }


}