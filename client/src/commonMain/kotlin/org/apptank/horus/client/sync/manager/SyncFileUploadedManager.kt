package org.apptank.horus.client.sync.manager

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.logException
import org.apptank.horus.client.extensions.warn
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository

class SyncFileUploadedManager(
    private val networkValidator: INetworkValidator,
    private val uploadFileRepository: IUploadFileRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var isReady: Boolean = false

    init {
        networkValidator.onNetworkChange {
            syncFiles()
        }

        EventBus.register(EventType.ON_READY) {
            isReady = true
        }
    }

    fun syncFiles() {

        if (!isReady) {
            info("[SyncFiles] Horus is not ready")
            return
        }

        if (!networkValidator.isNetworkAvailable()) {
            info("[SyncFiles] Network is not available")
            return
        }

        if (isTakenProcess) {
            warn("[SyncFiles] already in progress")
            return
        }

        scope.apply {

            val job = launch {
                takeProcess()
                // 1 -> Upload pending files
                uploadFileRepository.uploadFiles()
            }

            job.invokeOnCompletion {
                it?.let {
                    logException("[SyncFiles] Error while uploading files", it)
                }
                job.cancel()
                releaseProcess()
            }
        }

    }

    private fun takeProcess() {
        isTakenProcess = true
    }

    private fun releaseProcess() {
        isTakenProcess = false
    }

    companion object {
        private var isTakenProcess = false
    }
}