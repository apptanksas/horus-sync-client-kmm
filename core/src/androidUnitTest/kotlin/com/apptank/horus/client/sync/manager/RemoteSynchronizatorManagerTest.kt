package com.apptank.horus.client.sync.manager

import com.apptank.horus.client.TestCase
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random


class RemoteSynchronizatorManagerTest : TestCase() {


    @Mock
    val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    val syncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

    private val eventBus = EventBus

    private lateinit var remoteSynchronizatorManager: RemoteSynchronizatorManager

    @Before
    fun setup() {
        remoteSynchronizatorManager = RemoteSynchronizatorManager(
            networkValidator,
            syncControlDatabaseHelper,
            synchronizationService,
            eventBus,
            Dispatchers.Default,
            0
        )
    }

    @Test
    fun trySynchronizeDataNotExecuteByNetworkNoAvailable() {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(false)

        // When
        remoteSynchronizatorManager.trySynchronizeData()

        // Then
        verify { syncControlDatabaseHelper.getPendingActions() }.wasNotInvoked()
    }

    @Test
    fun trySynchronizeDataNotPendingActions() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { syncControlDatabaseHelper.getPendingActions() }.returns(emptyList())

        // When
        remoteSynchronizatorManager.trySynchronizeData()

        // Then
        coVerify { synchronizationService.postQueueActions(any()) }.wasNotInvoked()
    }

    @Test
    fun trySynchronizeDataPostQueueActionsIsFailure() = runBlocking {

        // Given
        val actions = generateArray {
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
        var eventCounter = 0

        eventBus.register(EventType.SYNC_PUSH_FAILED) {
            eventCounter++
        }

        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { syncControlDatabaseHelper.getPendingActions() }.returns(actions)
        coEvery { synchronizationService.postQueueActions(any()) }.returns(
            DataResult.Failure(
                Exception()
            )
        )

        // When
        remoteSynchronizatorManager.trySynchronizeData()

        // Then
        delay(50)
        Assert.assertEquals(1, eventCounter)
    }

    @Test
    fun trySynchronizeDataCompleteActionsIsFailure() = runBlocking {
        // Given
        val actions = generateArray {
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
        var eventCounter = 0

        eventBus.register(EventType.SYNC_PUSH_FAILED) {
            eventCounter++
        }

        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { syncControlDatabaseHelper.getPendingActions() }.returns(actions)
        coEvery { synchronizationService.postQueueActions(any()) }.returns(
            DataResult.Success(Unit)
        )
        every { syncControlDatabaseHelper.completeActions(any()) }.returns(false)

        // When
        remoteSynchronizatorManager.trySynchronizeData()

        // Then
        delay(50)
        Assert.assertEquals(1, eventCounter)
    }

    @Test
    fun trySynchronizeDataCompleteIsSuccess() = runBlocking {
        // Given
        val actions = generateArray {
            SyncControl.Action(
                Random.nextInt(), SyncControl.ActionType.UPDATE,
                "entity",
                SyncControl.ActionStatus.PENDING,
                emptyMap(), Clock.System.now()
                    .toLocalDateTime(
                        TimeZone.UTC
                    )
            )
        }
        var eventCounter = 0

        eventBus.register(EventType.SYNC_PUSH_SUCCESS) {
            eventCounter++
        }

        every { networkValidator.isNetworkAvailable() }.returns(true)
        every { syncControlDatabaseHelper.getPendingActions() }.returns(actions)
        coEvery { synchronizationService.postQueueActions(any()) }.returns(
            DataResult.Success(Unit)
        )
        every { syncControlDatabaseHelper.completeActions(any()) }.returns(true)

        // When
        remoteSynchronizatorManager.trySynchronizeData()

        // Then
        delay(50)
        Assert.assertEquals(1, eventCounter)
        verify { networkValidator.isNetworkAvailable() }.wasInvoked(1)
    }

    @Test
    fun trySynchronizeDataWhenOnNetworkChange() = runBlocking {
        // Given
        verify { networkValidator.onNetworkChange(any()) }.wasInvoked(1)
    }
}