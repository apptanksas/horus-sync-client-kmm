package org.apptank.horus.client.sync.manager

import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.sync.network.service.ISynchronizationService
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
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository
import org.junit.After
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

    @Mock
    val mockUploadFileRepository = mock(classOf<IUploadFileRepository>())

    private val eventBus = EventBus

    private lateinit var remoteSynchronizatorManager: RemoteSynchronizatorManager

    @Before
    fun setup() {
        remoteSynchronizatorManager = RemoteSynchronizatorManager(
            networkValidator,
            syncControlDatabaseHelper,
            synchronizationService,
            mockUploadFileRepository,
            eventBus,
            Dispatchers.Default,
            0
        )

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        HorusContainer.setupSyncControlDatabaseHelper(syncControlDatabaseHelper)
    }

    @After
    fun tearDown() {
        HorusAuthentication.clearSession()
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
        val actions = generateRandomArray {
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
        val actions = generateRandomArray {
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
        val actions = generateRandomArray {
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
        every { mockUploadFileRepository.hasFilesToUpload() }.returns(false)
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