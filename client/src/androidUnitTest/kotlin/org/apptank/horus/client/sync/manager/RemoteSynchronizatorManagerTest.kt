package org.apptank.horus.client.sync.manager

import com.russhwolf.settings.Settings
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.bus.InternalEventBus
import org.apptank.horus.client.bus.EventType
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import dev.mokkery.verify.VerifyMode.Companion.exactly
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

    val networkValidator = mock<INetworkValidator>(MockMode.autofill)
    val syncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
    val synchronizationService = mock<ISynchronizationService>(MockMode.autofill)
    val mockUploadFileRepository = mock<IUploadFileRepository>(MockMode.autofill)
    val storageSettings = mock<Settings>(MockMode.autofill)

    private val eventBus = InternalEventBus
    private lateinit var pushDataRemoteSynchronizatorManager: PushDataRemoteSynchronizatorManager

    @Before
    fun setup() {
        pushDataRemoteSynchronizatorManager = PushDataRemoteSynchronizatorManager(
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
        HorusContainer.setupSettings(storageSettings)
        HorusContainer.setupConfig(getHorusConfigTest())
    }

    @After
    fun tearDown() {
        HorusAuthentication.clearSession()
    }

    @Test
    fun trySynchronizeDataNotExecuteByNetworkNoAvailable() {
        every { networkValidator.isNetworkAvailable() } returns false

        pushDataRemoteSynchronizatorManager.trySynchronizeData()

        verify(exactly(0)) { syncControlDatabaseHelper.getPendingActions() }
    }

    @Test
    fun trySynchronizeDataNotPendingActions() = runBlocking {
        every { networkValidator.isNetworkAvailable() } returns true
        every { syncControlDatabaseHelper.getPendingActions() } returns emptyList()

        pushDataRemoteSynchronizatorManager.trySynchronizeData()

        verifySuspend(exactly(0)) { synchronizationService.postQueueActions(any()) }
    }

    @Test
    fun trySynchronizeDataPostQueueActionsIsFailure() = runBlocking {
        val actions = generateRandomArray {
            SyncControl.Action(
                Random.nextInt(), SyncControl.ActionType.INSERT,
                "entity", SyncControl.ActionStatus.PENDING,
                emptyMap(), Clock.System.now().toLocalDateTime(TimeZone.UTC)
            )
        }
        var eventCounter = 0
        eventBus.register(EventType.SYNC_PUSH_FAILED) { eventCounter++ }

        every { networkValidator.isNetworkAvailable() } returns true
        every { syncControlDatabaseHelper.getPendingActions() } returns actions
        everySuspend { synchronizationService.postQueueActions(any()) } returns DataResult.Failure(Exception())

        pushDataRemoteSynchronizatorManager.trySynchronizeData()

        delay(50)
        Assert.assertEquals(1, eventCounter)
    }

    @Test
    fun trySynchronizeDataCompleteActionsIsFailure() = runBlocking {
        val actions = generateRandomArray {
            SyncControl.Action(
                Random.nextInt(), SyncControl.ActionType.INSERT,
                "entity", SyncControl.ActionStatus.PENDING,
                emptyMap(), Clock.System.now().toLocalDateTime(TimeZone.UTC)
            )
        }
        var eventCounter = 0
        eventBus.register(EventType.SYNC_PUSH_FAILED) { eventCounter++ }

        every { networkValidator.isNetworkAvailable() } returns true
        every { syncControlDatabaseHelper.getPendingActions() } returns actions
        everySuspend { synchronizationService.postQueueActions(any()) } returns DataResult.Success(Unit)
        every { syncControlDatabaseHelper.completeActions(any()) } returns false

        pushDataRemoteSynchronizatorManager.trySynchronizeData()

        delay(50)
        Assert.assertEquals(1, eventCounter)
    }

    @Test
    fun trySynchronizeDataCompleteIsSuccess() = runBlocking {
        val actions = generateRandomArray {
            SyncControl.Action(
                Random.nextInt(), SyncControl.ActionType.UPDATE,
                "entity", SyncControl.ActionStatus.PENDING,
                emptyMap(), Clock.System.now().toLocalDateTime(TimeZone.UTC)
            )
        }
        var eventCounter = 0
        eventBus.register(EventType.SYNC_PUSH_SUCCESS) { eventCounter++ }

        every { networkValidator.isNetworkAvailable() } returns true
        every { syncControlDatabaseHelper.getPendingActions() } returns actions
        everySuspend { synchronizationService.postQueueActions(any()) } returns DataResult.Success(Unit)
        every { mockUploadFileRepository.hasFilesToUpload() } returns false
        every { syncControlDatabaseHelper.completeActions(any()) } returns true

        pushDataRemoteSynchronizatorManager.trySynchronizeData()

        delay(50)
        Assert.assertEquals(1, eventCounter)
        verify(exactly(1)) { networkValidator.isNetworkAvailable() }
    }
}
