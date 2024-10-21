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
import org.apptank.horus.client.sync.upload.data.SyncFileResult
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository

class SyncFileUploadedManager(
    private val networkValidator: INetworkValidator,
    private val uploadFileRepository: IUploadFileRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
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
                info("[SyncFiles] Uploading files...")
                if (uploadFileRepository.uploadFiles().isSuccess()) {
                    info("[SyncFiles] Files uploaded successfully")
                    // 2 -> Sync file references info
                    info("[SyncFiles] Syncing file references info...")
                    if (uploadFileRepository.syncFileReferencesInfo()) {
                        info("[SyncFiles] File references info synced successfully")
                        // 3 -> download files
                        info("[SyncFiles] Downloading files...")
                        if (uploadFileRepository.downloadRemoteFiles().isSuccess()) {
                            info("[SyncFiles] Files downloaded successfully")
                        } else {
                            logException("[SyncFiles] Error while downloading files")
                        }
                    } else {
                        logException("[SyncFiles] Error while syncing file references info")
                    }

                } else {
                    logException("[SyncFiles] Error while uploading files")
                }
            }

            job.invokeOnCompletion {
                it?.let {
                    logException("[SyncFiles] Error while uploading files", it)
                } ?: info("[SyncFiles] Sync files completed")
                job.cancel()
                releaseProcess()
            }
        }

    }

    private fun List<SyncFileResult>.isSuccess(): Boolean {
        return this.all { it is SyncFileResult.Success }
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