package org.apptank.horus.client.sync.manager

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.base.SuspendedCallback
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.isFalse
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
            syncFiles()
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
                uploadFiles { syncFileReferencesInfo { downloadFiles { releaseProcess() } } }
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

    private suspend fun uploadFiles(onContinue: SuspendedCallback) {
        info("[SyncFiles] Uploading files...")

        val uploadFilesResult = uploadFileRepository.uploadFiles()

        if (uploadFilesResult.isFailure()) {
            logException("[SyncFiles] Error while uploading files")
            return
        }

        printLogUploadFilesResult(uploadFilesResult)
        onContinue()
    }

    private suspend fun syncFileReferencesInfo(onContinue: SuspendedCallback) {
        info("[SyncFiles] Syncing file references info...")

        if (uploadFileRepository.syncFileReferencesInfo().isFalse()) {
            logException("[SyncFiles] Error while syncing file references info")
            return
        }

        info("[SyncFiles] File references info synced successfully")
        onContinue()
    }

    private suspend fun downloadFiles(onContinue: SuspendedCallback) {
        info("[SyncFiles] Downloading files...")
        val downloadFilesResult = uploadFileRepository.downloadRemoteFiles()

        if (downloadFilesResult.isFailure()) {
            logException("[SyncFiles] Error while downloading files")
            return
        }

        printLogDownloadFilesResult(downloadFilesResult)
        onContinue()
    }

    private fun printLogUploadFilesResult(uploadFilesResult: List<SyncFileResult>) {
        if (uploadFilesResult.countSuccess() > 0) {
            info("[SyncFiles] Files uploaded successfully. [Success: ${uploadFilesResult.countSuccess()}]")
        } else {
            info("[SyncFiles] No files to upload.")
        }
    }

    private fun printLogDownloadFilesResult(downloadFilesResult: List<SyncFileResult>) {
        if (downloadFilesResult.countSuccess() > 0) {
            info("[SyncFiles] Files downloaded successfully. [Success: ${downloadFilesResult.countSuccess()}]")
        } else {
            info("[SyncFiles] No files to download.")
        }
    }

    private fun List<SyncFileResult>.isFailure(): Boolean {
        return this.any { it is SyncFileResult.Failure }
    }

    private fun List<SyncFileResult>.countSuccess(): Int {
        return this.count { it is SyncFileResult.Success }
    }

    private fun List<SyncFileResult>.countFailures(): Int {
        return this.count { it is SyncFileResult.Failure }
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