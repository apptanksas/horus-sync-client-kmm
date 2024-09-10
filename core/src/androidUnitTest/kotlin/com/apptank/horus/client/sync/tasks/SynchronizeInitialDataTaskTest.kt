package com.apptank.horus.client.sync.tasks

import com.apptank.horus.client.MOCK_RESPONSE_GET_DATA
import com.apptank.horus.client.TestCase
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.buildEntitiesDataFromJSON
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.database.IOperationDatabaseHelper
import com.apptank.horus.client.di.INetworkValidator
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import com.apptank.horus.client.tasks.SynchronizeInitialDataTask
import com.apptank.horus.client.tasks.TaskResult
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test


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
        val result = task.execute(null)

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

            // When
            val result = task.execute(null)

            // Then
            assert(result is TaskResult.Failure)
        }

    @Test
    fun `when initial synchronization is not completed then synchronize data is success`(): Unit =
        runBlocking {
            // Given
            val entitiesData =
                buildEntitiesDataFromJSON(MOCK_RESPONSE_GET_DATA)
            every { networkValidator.isNetworkAvailable() }.returns(true)
            every { syncControlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION) }
                .returns(false)
            coEvery { synchronizeService.getData() }.returns(DataResult.Success(entitiesData))
            every {
                operationDatabaseHelper.insertWithTransaction(
                    any(),
                    callbackMatcher()
                )
            }.returns(true)

            // When
            val result = task.execute(null)

            // Then
            assert(result is TaskResult.Success)
            verify { operationDatabaseHelper.insertWithTransaction(any(), any()) }.wasInvoked()
            verify {
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.INITIAL_SYNCHRONIZATION,
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
        val result = task.execute(null)

        // Then
        assert(result is TaskResult.Failure)
        coVerify { synchronizeService.getData(any()) }.wasNotInvoked()
    }


}