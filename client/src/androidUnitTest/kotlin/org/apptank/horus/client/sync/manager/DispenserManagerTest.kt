package org.apptank.horus.client.sync.manager

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
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

    val networkValidator = mock<INetworkValidator>(MockMode.autofill)
    val syncControlDatabaseHelper = mock<ISyncControlDatabaseHelper>(MockMode.autofill)
    val synchronizationService = mock<ISynchronizationService>(MockMode.autofill)
    val mockUploadFileRepository = mock<IUploadFileRepository>(MockMode.autofill)
    val storageSettings = mock<Settings>(MockMode.autofill)

    private val eventBus = InternalEventBus

    private lateinit var pushDataRemoteSynchronizatorManager: PushDataRemoteSynchronizatorManager
    private lateinit var dispenserManager: DispenserManager

    private val BATCH_SIZE = 10
    private val EXPIRATION_TIME_SECONDS = 60L * 60 * 24 // 24 hours

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
        dispenserManager = DispenserManager(
            BATCH_SIZE,
            EXPIRATION_TIME_SECONDS,
            syncControlDatabaseHelper,
            pushDataRemoteSynchronizatorManager
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

        every { networkValidator.isNetworkAvailable() } returns false
        every { syncControlDatabaseHelper.getLastActionCompleted() } returns null
        every { syncControlDatabaseHelper.getPendingActions() } returns actions

        // When
        for (i in 1..BATCH_SIZE) {
            dispenserManager.processBatch()
        }

        // Then
        verify(exactly(1)) { networkValidator.isNetworkAvailable() }
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

        every { networkValidator.isNetworkAvailable() } returns false
        every { syncControlDatabaseHelper.getLastActionCompleted() } returns actions.last()
        every { syncControlDatabaseHelper.getPendingActions() } returns actions

        // When
        dispenserManager.processBatch()

        // Then
        verify(exactly(1)) { networkValidator.isNetworkAvailable() }
    }
}