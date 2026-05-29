package org.apptank.horus.client.tasks

import com.russhwolf.settings.Settings
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test


class SynchronizeDataTaskTest : TestCase() {

    val networkValidator = mock<INetworkValidator>(MockMode.autofill)
    val controlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
    val operationDatabaseHelper = mock<IOperationDatabaseHelper>(MockMode.autofill)
    val synchronizationService = mock<ISynchronizationService>(MockMode.autofill)
    val storageSettings = mock<Settings>(MockMode.autofill)

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

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        HorusContainer.setupSyncControlDatabaseHelper(controlDatabaseHelper)
        HorusContainer.setupSettings(storageSettings)
    }

    @After
    fun tearDown() {
        HorusAuthentication.clearSession()
    }

    @Test
    fun `when synchronization is idle then return success`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() } returns false

        // When
        val result = task.execute(null, 0, 10)

        // Then
        assert(result is TaskResult.Success)
        verify(exactly(0)) { controlDatabaseHelper.getPendingActions() }
    }

    @Test
    fun `when synchronization is failure then return failure`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() } returns true
        every { controlDatabaseHelper.getPendingActions() } returns emptyList()
        every { controlDatabaseHelper.getLastDatetimeCheckpoint() } returns 0
        every { controlDatabaseHelper.getCompletedActionsAfterDatetime(any()) } returns emptyList()

        everySuspend { synchronizationService.getQueueActions(any(), any()) } returns DataResult.Failure(Exception())

        // When
        val result = task.execute(null, 0, 10)

        // Then
        assert(result is TaskResult.Failure)
        verify(exactly(0)) { controlDatabaseHelper.getEntityNames() }
    }

    @Test
    fun `when synchronization is success then return success`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() } returns true
        every { controlDatabaseHelper.getPendingActions() } returns emptyList()
        every { controlDatabaseHelper.getLastDatetimeCheckpoint() } returns 0
        every { controlDatabaseHelper.getCompletedActionsAfterDatetime(any()) } returns emptyList()
        every { controlDatabaseHelper.getExistsActionSequences(any()) } returns emptyList()
        everySuspend { synchronizationService.getQueueActions(any(),any()) } returns DataResult.Success(emptyList())

        everySuspend { synchronizationService.getQueueActions(any(), any()) } returns DataResult.Success(
            listOf(SyncDTO.Response.SyncAction())
        )

        // When
        val result = task.execute(null, 0, 10)

        // Then
        assert(result is TaskResult.Success)
        verify(exactly(0)) { controlDatabaseHelper.getEntityNames() }
    }

}