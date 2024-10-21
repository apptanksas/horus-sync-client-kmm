package org.apptank.horus.client.sync.manager

import io.mockative.Mock
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.every
import io.mockative.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.sync.upload.data.SyncFileResult
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository
import org.junit.Before
import org.junit.Test


class SyncFileUploadedManagerTest {

    @Mock
    private val networkValidator: INetworkValidator = mock(classOf<INetworkValidator>())

    @Mock
    private val repository: IUploadFileRepository = mock(classOf<IUploadFileRepository>())

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
        coVerify { repository.uploadFiles() }.wasNotInvoked()
    }

    @Test
    fun `syncFiles should not proceed if network is not available`() = runBlocking {
        // Given
        every { networkValidator.isNetworkAvailable() }.returns(false)
        // When
        manager.syncFiles()
        // Then
        coVerify { repository.uploadFiles() }.wasNotInvoked()
    }

    @Test
    fun `syncFiles is success`() = runBlocking {
        // Given
        EventBus.emit(EventType.ON_READY)
        every { networkValidator.isNetworkAvailable() }.returns(true)
        coEvery { repository.uploadFiles() }.returns(listOf(SyncFileResult.Success("file1")))
        coEvery { repository.syncFileReferencesInfo() }.returns(true)
        coEvery { repository.downloadRemoteFiles() }.returns(listOf(SyncFileResult.Success("file2")))

        // When
        manager.syncFiles()

        // Then
        delay(100)
        coVerify { repository.uploadFiles() }.wasInvoked()
        coVerify { repository.syncFileReferencesInfo() }.wasInvoked()
        coVerify { repository.downloadRemoteFiles() }.wasInvoked()
    }

}