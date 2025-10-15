package org.apptank.horus.client.sync.manager

import io.mockative.Mock
import io.mockative.classOf
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.bus.InternalEventBus
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.random.Random
import com.russhwolf.settings.Settings

class DispenserManagerTest : TestCase() {

    @Mock
    val networkValidator = mock(classOf<INetworkValidator>())

    @Mock
    val syncControlDatabaseHelper = mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    val synchronizationService = mock(classOf<ISynchronizationService>())

    @Mock
    val mockUploadFileRepository = mock(classOf<IUploadFileRepository>())

    @Mock
    val storageSettings = mock(classOf<Settings>())

    private val eventBus = InternalEventBus

    private lateinit var remoteSynchronizatorManager: RemoteSynchronizatorManager
    private lateinit var dispenserManager: DispenserManager

    private val BATCH_SIZE = 10
    private val EXPIRATION_TIME_SECONDS = 60L * 60 * 24 // 24 hours

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
        dispenserManager = DispenserManager(
            BATCH_SIZE,
            EXPIRATION_TIME_SECONDS,
            syncControlDatabaseHelper,
            remoteSynchronizatorManager
        )

        HorusAuthentication.setupUserAccessToken(USER_ACCESS_TOKEN)
        HorusContainer.setupSyncControlDatabaseHelper(syncControlDatabaseHelper)
        HorusContainer.setupSettings(storageSettings)
    }

    @After
    fun tearDown() {
        HorusAuthentication.clearSession()
    }


    @Test
    fun `when processBatch reach batch size then synchronize`() {
        // Given
        val actionedAt = Clock.System.now().epochSeconds
        val actions = generateArray(BATCH_SIZE) {
            SyncControl.Action(
                Random.nextInt(), SyncControl.ActionType.INSERT,
                "entity",
                SyncControl.ActionStatus.PENDING,
                mutableMapOf(),
                Instant.fromEpochSeconds(actionedAt).toLocalDateTime(TimeZone.UTC)
            )
        }

        every { networkValidator.isNetworkAvailable() }.returns(false)
        every { syncControlDatabaseHelper.getLastActionCompleted() }.returns(null)
        every { syncControlDatabaseHelper.getPendingActions() }.returns(actions)

        // When
        for (i in 1..BATCH_SIZE) {
            dispenserManager.processBatch()
        }

        // Then
        verify { networkValidator.isNetworkAvailable() }.wasInvoked(1)
    }

    @Test
    fun `when processBatch reach expiration time then synchronize`() {
        // Given
        val actionedAt = Clock.System.now().epochSeconds - EXPIRATION_TIME_SECONDS
        val actions = generateRandomArray(BATCH_SIZE) {
            SyncControl.Action(
                Random.nextInt(), SyncControl.ActionType.INSERT,
                "entity",
                SyncControl.ActionStatus.PENDING,
                mutableMapOf(),
                Instant.fromEpochSeconds(actionedAt).toLocalDateTime(TimeZone.UTC)
            )
        }

        every { networkValidator.isNetworkAvailable() }.returns(false)
        every { syncControlDatabaseHelper.getLastActionCompleted() }.returns(actions.last())
        every { syncControlDatabaseHelper.getPendingActions() }.returns(actions)

        // When
        dispenserManager.processBatch()

        // Then
        verify { networkValidator.isNetworkAvailable() }.wasInvoked(1)
    }
}