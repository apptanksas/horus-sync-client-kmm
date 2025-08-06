package org.apptank.horus.client.sync.manager

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.base.DataResult
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

/**
 * SyncFileUploadedManager is responsible for synchronizing uploaded files with the remote server.
 *
 * The class listens for network changes and Horus readiness events to trigger the synchronization process.
 *
 * @param networkValidator The network validator to check network availability.
 * @param uploadFileRepository The repository for uploading files.
 * @param dispatcher The coroutine dispatcher for the manager.
 *
 * @author John Ospina
 * @year 2024
 */
class SyncFileUploadedManager(
    private val networkValidator: INetworkValidator,
    private val uploadFileRepository: IUploadFileRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ISyncFileUploadedManager {

    // Coroutine scope for the manager
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

        EventBus.register(EventType.USER_SESSION_CLEARED) {
            isReady = false
        }
    }

    /**
     * Synchronizes uploaded files with the remote server.
     *
     * The method checks if Horus is ready and the network is available before starting the synchronization process.
     * The method is idempotent and will not start a new synchronization process if one is already in progress.
     *
     * @param onCompleted The callback to execute when the synchronization process is completed.
     */
    override fun syncFiles(onCompleted: Callback) {

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
                    logException("[SyncFiles] Error sync files", it)
                } ?: info("[SyncFiles] Sync files completed")
                job.cancel()
                releaseProcess()
                onCompleted()
            }
        }
    }

    /**
     * Synchronizes uploaded files with the remote server.
     *
     * The method checks if Horus is ready and the network is available before starting the synchronization process.
     * The method is idempotent and will not start a new synchronization process if one is already in progress.
     */
    private suspend fun uploadFiles(onContinue: SuspendedCallback) {
        info("[SyncFiles] Uploading files...")

        val uploadFilesResult = uploadFileRepository.uploadFiles()

        if (uploadFilesResult.isFailure()) {
            uploadFilesResult.forEach {
                val failure = it as SyncFileResult.Failure
                logException(
                    "[SyncFiles] Error while uploading files -> ${failure.exception.message}",
                    failure.exception
                )
            }
            EventBus.emit(EventType.SYNC_PUSH_FAILED)
            return
        }

        printLogUploadFilesResult(uploadFilesResult)
        onContinue()
    }

    /**
     * Synchronizes file references info with the remote server.
     *
     * @param onContinue The callback to execute when the synchronization process is completed.
     */
    private suspend fun syncFileReferencesInfo(onContinue: SuspendedCallback) {
        info("[SyncFiles] Syncing file references info...")

        if (uploadFileRepository.syncFileReferencesInfo().isFalse()) {
            logException("[SyncFiles] Error while syncing file references info")
            return
        }

        info("[SyncFiles] File references info synced successfully")
        onContinue()
    }

    /**
     * Downloads files from the remote server.
     *
     * @param onContinue The callback to execute when the synchronization process is completed.
     */
    private suspend fun downloadFiles(onContinue: SuspendedCallback) {
        info("[SyncFiles] Downloading files...")
        val downloadFilesResult = uploadFileRepository.downloadRemoteFiles()

        if (downloadFilesResult.isFailure()) {
            downloadFilesResult.forEach {
                val failure = it as SyncFileResult.Failure
                logException(
                    "[SyncFiles] Error while downloading files -> ${failure.exception.message}",
                    failure.exception
                )
            }
            return
        }

        printLogDownloadFilesResult(downloadFilesResult)
        onContinue()
    }

    /**
     * Prints the result of the upload files process.
     *
     * @param uploadFilesResult The list of results from the upload files process.
     */
    private fun printLogUploadFilesResult(uploadFilesResult: List<SyncFileResult>) {
        if (uploadFilesResult.countSuccess() > 0) {
            info("[SyncFiles] Files uploaded successfully. [Success: ${uploadFilesResult.countSuccess()}]")
        } else {
            info("[SyncFiles] No files to upload.")
        }
    }

    /**
     * Prints the result of the download files process.
     *
     * @param downloadFilesResult The list of results from the download files process.
     */
    private fun printLogDownloadFilesResult(downloadFilesResult: List<SyncFileResult>) {
        if (downloadFilesResult.countSuccess() > 0) {
            info("[SyncFiles] Files downloaded successfully. [Success: ${downloadFilesResult.countSuccess()}]")
        } else {
            info("[SyncFiles] No files to download.")
        }
    }

    /**
     * Checks if the list of SyncFileResult contains any failure.
     *
     * @return True if the list contains any failure, false otherwise.
     */
    private fun List<SyncFileResult>.isFailure(): Boolean {
        return this.any { it is SyncFileResult.Failure }
    }

    /**
     * Counts the number of successful results in the list of SyncFileResult.
     *
     * @return The number of successful results.
     */
    private fun List<SyncFileResult>.countSuccess(): Int {
        return this.count { it is SyncFileResult.Success }
    }

    /**
     * Counts the number of failed results in the list of SyncFileResult.
     *
     * @return The number of failed results.
     */
    private fun List<SyncFileResult>.countFailures(): Int {
        return this.count { it is SyncFileResult.Failure }
    }

    /**
     * Takes the synchronization process.
     */
    private fun takeProcess() {
        isTakenProcess = true
    }

    /**
     * Releases the synchronization process.
     */
    private fun releaseProcess() {
        isTakenProcess = false
    }

    companion object {
        private var isTakenProcess = false
    }
}