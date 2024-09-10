package com.apptank.horus.client.tasks

import com.apptank.horus.client.TestCase
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.database.IOperationDatabaseHelper
import com.apptank.horus.client.di.INetworkValidator
import com.apptank.horus.client.sync.network.dto.SyncDTO
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test


class SynchronizeDataTaskTest : TestCase() {
    @Mock
    val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    val controlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    val operationDatabaseHelper = mock(classOf<IOperationDatabaseHelper>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

    private lateinit var task: SynchronizeDataTask

    @Before
    fun setup() {
        task = SynchronizeDataTask(
            networkValidator,
            controlDatabaseHelper,
            operationDatabaseHelper,
            synchronizationService,
            getMockSynchronizeInitialDataTask()
        )
    }

    @Test
    fun `when synchronization is idle then return success`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(false)

        // When
        val result = task.execute(null)

        // Then
        assert(result is TaskResult.Success)
        verify { controlDatabaseHelper.getPendingActions() }.wasNotInvoked()
    }

    @Test
    fun `when synchronization is failure then return failure`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { controlDatabaseHelper.getPendingActions() }.returns(emptyList())
        every { controlDatabaseHelper.getLastDatetimeCheckpoint() }.returns(0)
        every { controlDatabaseHelper.getCompletedActionsAfterDatetime(any()) }.returns(emptyList())

        coEvery { synchronizationService.getQueueActions(any(), any()) }
            .returns(DataResult.Failure(Exception()))

        // When
        val result = task.execute(null)

        // Then
        assert(result is TaskResult.Failure)
        verify { controlDatabaseHelper.getEntityNames() }.wasNotInvoked()
    }

    @Test
    fun `when synchronization is success then return success`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { controlDatabaseHelper.getPendingActions() }.returns(emptyList())
        every { controlDatabaseHelper.getLastDatetimeCheckpoint() }.returns(0)
        every { controlDatabaseHelper.getCompletedActionsAfterDatetime(any()) }.returns(emptyList())

        coEvery { synchronizationService.getQueueActions(any(), any()) }
            .returns(DataResult.Success(listOf(SyncDTO.Response.SyncAction())))

        // When
        val result = task.execute(null)

        // Then
        assert(result is TaskResult.Success)
        verify { controlDatabaseHelper.getEntityNames() }.wasNotInvoked()
    }


}