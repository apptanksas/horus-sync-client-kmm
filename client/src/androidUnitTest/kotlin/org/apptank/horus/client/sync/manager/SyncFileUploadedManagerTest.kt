package org.apptank.horus.client.sync.manager

import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.every
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.bus.InternalEventBus
import org.apptank.horus.client.bus.EventType
import org.apptank.horus.client.sync.upload.data.SyncFileResult
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SyncFileUploadedManagerTest {

    private val networkValidator = mock<INetworkValidator>(MockMode.autofill)
    private val repository = mock<IUploadFileRepository>(MockMode.autofill)

    private lateinit var manager: SyncFileUploadedManager

    @Before
    fun setUp() {
        manager = SyncFileUploadedManager(
            networkValidator,
            repository,
            Dispatchers.Default
        )
    }

    @Test
    fun `syncFiles should not proceed if Horus is not ready`() = runBlocking {
        // When
        manager.syncFiles()
        // Then
        verifySuspend(exactly(0)) { repository.uploadFiles() }
    }

    @Test
    fun `syncFiles should not proceed if network is not available`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() } returns false
        // When
        manager.syncFiles()
        // Then
        verifySuspend(exactly(0)) { repository.uploadFiles() }
    }

    @Test
    fun `syncFiles is success when is ready`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { repository.uploadFiles() } returns listOf(SyncFileResult.Success("file1"))
        everySuspend { repository.syncFileReferencesInfo() } returns true
        everySuspend { repository.downloadRemoteFiles() } returns listOf(SyncFileResult.Success("file2"))

        // When
        InternalEventBus.emit(EventType.ON_READY)
        manager.syncFiles()

        // Then
        delay(100)
        verifySuspend { repository.uploadFiles() }
        verifySuspend { repository.syncFileReferencesInfo() }
        verifySuspend { repository.downloadRemoteFiles() }
    }

}