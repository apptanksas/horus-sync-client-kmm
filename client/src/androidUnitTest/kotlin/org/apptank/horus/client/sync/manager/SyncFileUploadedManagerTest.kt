package org.apptank.horus.client.sync.manager

import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.every
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertTrue


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

    @Test
    fun `syncFiles when callback triggers another syncFiles should execute all callbacks without ConcurrentModificationException`() = runBlocking {
        val unconfinedManager = SyncFileUploadedManager(
            networkValidator,
            repository,
            Dispatchers.Unconfined
        )

        var uploadCallCount = 0
        val firstDeferredUpload = CompletableDeferred<List<SyncFileResult>>()
        val secondDeferredUpload = CompletableDeferred<List<SyncFileResult>>()

        // Given
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { repository.uploadFiles() } calls {
            uploadCallCount++
            if (uploadCallCount == 1) {
                firstDeferredUpload.await()
            } else {
                secondDeferredUpload.await()
            }
        }
        everySuspend { repository.syncFileReferencesInfo() } returns true
        everySuspend { repository.downloadRemoteFiles() } returns listOf(SyncFileResult.Success("file2"))

        // When
        InternalEventBus.emit(EventType.ON_READY)

        var firstCallbackExecuted = false
        var secondCallbackExecuted = false
        var thirdCallbackExecuted = false

        // Start first sync
        unconfinedManager.syncFiles {
            firstCallbackExecuted = true
            // Inside first callback, trigger another syncFiles
            unconfinedManager.syncFiles {
                thirdCallbackExecuted = true
            }
        }

        // Queue second callback while first sync is in progress
        unconfinedManager.syncFiles {
            secondCallbackExecuted = true
        }

        // Complete the first upload to trigger releaseProcess
        firstDeferredUpload.complete(listOf(SyncFileResult.Success("file1")))

        // Verify first batch callbacks ran without ConcurrentModificationException
        assertTrue(firstCallbackExecuted)
        assertTrue(secondCallbackExecuted)

        // Complete the second upload to trigger the re-entrant sync's releaseProcess
        secondDeferredUpload.complete(listOf(SyncFileResult.Success("file1")))
        assertTrue(thirdCallbackExecuted)
    }

    @Test
    fun `syncFiles when called concurrently should not throw ConcurrentModificationException`() = runBlocking {
        every { networkValidator.isNetworkAvailable() } returns true
        everySuspend { repository.uploadFiles() } returns listOf(SyncFileResult.Success("file1"))
        everySuspend { repository.syncFileReferencesInfo() } returns true
        everySuspend { repository.downloadRemoteFiles() } returns listOf(SyncFileResult.Success("file2"))

        InternalEventBus.emit(EventType.ON_READY)

        val exceptions = java.util.Collections.synchronizedList(mutableListOf<Throwable>())
        val threads = (1..50).map { index ->
            Thread {
                try {
                    manager.syncFiles {
                        // callback might trigger another sync
                        if (index % 2 == 0) {
                            manager.syncFiles()
                        }
                    }
                } catch (e: Throwable) {
                    exceptions.add(e)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }
        delay(500)

        assertTrue(exceptions.isEmpty(), "Exceptions occurred: ${exceptions.map { it.stackTraceToString() }}")
    }

}