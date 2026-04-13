package org.apptank.horus.client.sync.tasks

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.mokkery.answering.returns
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
import dev.mokkery.answering.returns
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matches
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.apptank.horus.client.MOCK_RESPONSE_GET_SYNC_STATUS
import org.apptank.horus.client.MOCK_RESPONSE_SYNC_DATA_FILE
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.buildSyncDataStatusFromJSON
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlin.random.nextUInt


class SynchronizeInitialDataTaskTest : TestCase() {

    private val operationDatabaseHelper = mock<IOperationDatabaseHelper>(MockMode.autofill)
    private val syncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
    private val synchronizeService = mock<ISynchronizationService>(MockMode.autofill)
    private val networkValidator = mock<INetworkValidator>(MockMode.autofill)

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
        every { syncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns true

        val result = task.execute(null, 0, 10)

        assert(result is TaskResult.Success)
        verifySuspend(dev.mokkery.verify.VerifyMode.exactly(0)) { synchronizeService.getData(any()) }
    }

    @Test
    fun `when initial synchronization is not completed then synchronize data is failure`(): Unit = runBlocking {
        every { networkValidator.isNetworkAvailable() } returns true
        every { syncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns false
        everySuspend { synchronizeService.getData() } returns DataResult.Failure(Exception("Error synchronizing data"))
        everySuspend { synchronizeService.postStartSync(any()) } returns DataResult.Failure(Exception("Error synchronizing data"))

        val result = task.execute(null, 0, 10)

        assert(result is TaskResult.Failure)
    }

    @Test
    fun `when initial synchronization is not completed then synchronize data is success`(): Unit = runBlocking {
        val syncDataStatus = buildSyncDataStatusFromJSON(MOCK_RESPONSE_GET_SYNC_STATUS)

        every { networkValidator.isNetworkAvailable() } returns true
        every { syncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns false

        every {
            operationDatabaseHelper.insertWithTransaction(any(), any())
        } calls { args ->
            (args.args[1] as Callback).invoke()
            true
        }

        everySuspend { synchronizeService.postStartSync(any()) } returns DataResult.Success(Unit)
        everySuspend { synchronizeService.getSyncStatus(any()) } returns DataResult.Success(syncDataStatus)
        every { syncControlDatabaseHelper.getEntityLevel(any()) } returns 1

        mockDownloadSyncData(syncDataStatus.downloadUrl)

        val result = task.execute(null, 0, 10)

        assert(result is TaskResult.Success)
        verify { operationDatabaseHelper.insertWithTransaction(any(), any()) }
        verify {
            syncControlDatabaseHelper.addSyncTypeStatus(
                SyncControl.OperationType.INITIAL_SYNCHRONIZATION,
                SyncControl.Status.COMPLETED
            )
        }
        verify {
            syncControlDatabaseHelper.addSyncTypeStatus(
                SyncControl.OperationType.CHECKPOINT,
                SyncControl.Status.COMPLETED
            )
        }
    }

    @Test
    fun `when network is not available then return failure`(): Unit = runBlocking {
        every { networkValidator.isNetworkAvailable() } returns false
        every { syncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) } returns false

        val result = task.execute(null, 0, 10)

        assert(result is TaskResult.Failure)
        verifySuspend(dev.mokkery.verify.VerifyMode.exactly(0)) { synchronizeService.getData(any()) }
    }

    @Test
    fun `when get data is success then migration success with database constraints`(): Unit = runBlocking {
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

        every { networkValidator.isNetworkAvailable() } returns true
        mockDownloadSyncData(syncDataStatus.downloadUrl)

        everySuspend { synchronizeService.postStartSync(any()) } returns DataResult.Success(Unit)
        everySuspend { synchronizeService.getSyncStatus(any()) } returns DataResult.Success(syncDataStatus)

        HorusDatabase.Schema.create(driver, entitiesScheme)

        val result = task.execute(null, 0, 10)

        assert(result is TaskResult.Success)
    }

    private suspend fun mockDownloadSyncData(url: String?) {
        val filename = "sync_data_" + (Random.nextUInt()) + ".ndjson"
        val pathFile = getLocalTestPath(filename)
        val path = createFileInLocalStorage(pathFile, MOCK_RESPONSE_SYNC_DATA_FILE).toPath()

        everySuspend {
            synchronizeService.downloadSyncData(matches { it == url }, matches { true })
        } returns DataResult.Success(path)
    }

}