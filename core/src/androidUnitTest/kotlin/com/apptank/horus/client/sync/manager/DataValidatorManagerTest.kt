package com.apptank.horus.client.sync.manager

import com.apptank.horus.client.TestCase
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.database.IOperationDatabaseHelper
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.sync.network.dto.SyncDTO
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.eq
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import kotlinx.datetime.toInstant

class DataValidatorManagerTest : TestCase() {

    @Mock
    val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    val operationDatabaseHelper = mock(classOf<IOperationDatabaseHelper>())

    @Mock
    val syncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

    lateinit var dataValidatorManager: DataValidatorManager

    @Before
    fun setup() {
        dataValidatorManager = DataValidatorManager(
            networkValidator,
            syncControlDatabaseHelper,
            operationDatabaseHelper,
            synchronizationService,
            Dispatchers.Default,
        )
    }

    @Test
    fun `when start with network is not available then do nothing`() {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(false)

        // When
        dataValidatorManager.start()

        // Then
        verify { syncControlDatabaseHelper.getPendingActions() }.wasNotInvoked()
    }

    @Test
    fun `when exists data pending to push then do nothing`() {
        // Given
        val actions = generateSyncActions()
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { syncControlDatabaseHelper.getPendingActions() }.returns(actions)

        // When
        dataValidatorManager.start()

        // Then
        verify { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }.wasNotInvoked()
    }

    @Test
    fun `when exists data to sync then do nothing`() = runBlocking {
        // Given
        val actions = generateSyncActions()
        val responseActions = generateResponseSyncActions()
        val checkpointTimestamp = Clock.System.now().toEpochMilliseconds()
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())
        every { syncControlDatabaseHelper.getLastDatetimeCheckpoint() }.returns(checkpointTimestamp)
        every { syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp) }.returns(
            actions
        )
        coEvery { synchronizationService.getQueueActions(eq(checkpointTimestamp), any()) }.returns(
            DataResult.Success(responseActions)
        )

        // When
        dataValidatorManager.start()

        // Then
        verify { syncControlDatabaseHelper.getEntityNames() }.wasNotInvoked()
    }

    private fun generateSyncActions(): List<SyncControl.Action> {
        return generateArray {
            SyncControl.Action(
                Random.nextInt(), SyncControl.ActionType.INSERT,
                "entity",
                SyncControl.ActionStatus.PENDING,
                emptyMap(), Clock.System.now()
                    .toLocalDateTime(
                        TimeZone.UTC
                    )
            )
        }
    }

    private fun generateResponseSyncActions(): List<SyncDTO.Response.SyncAction> {
        return generateSyncActions().map {
            SyncDTO.Response.SyncAction(
                it.action.name,
                it.entity,
                it.data,
                it.datetime.toInstant(TimeZone.UTC).epochSeconds,
                it.datetime.toInstant(TimeZone.UTC).epochSeconds
            )
        }
    }
}