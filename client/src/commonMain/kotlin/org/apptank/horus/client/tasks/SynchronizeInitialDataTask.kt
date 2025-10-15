package org.apptank.horus.client.tasks

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.fold
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.database.struct.toRecordsInsert
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.logException
import org.apptank.horus.client.serialization.AnySerializer
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.dto.toListEntityData
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A task responsible for performing the initial data synchronization.
 *
 * @property networkValidator Validator to check network connectivity.
 * @property operationDatabaseHelper Helper to interact with the operation database.
 * @property controlDatabaseHelper Helper to interact with the sync control database.
 * @property synchronizeService Service to handle synchronization operations.
 * @property dependsOnTask The task that must be completed before this task can run.
 * @param pollingTime The time to wait between retries for data synchronization (default is 5000 milliseconds).
 */
internal class SynchronizeInitialDataTask(
    private val networkValidator: INetworkValidator,
    private val operationDatabaseHelper: IOperationDatabaseHelper,
    private val controlDatabaseHelper: ISyncControlDatabaseHelper,
    private val synchronizeService: ISynchronizationService,
    dependsOnTask: ValidateHashingTask,
    private val pollingTime: Long = 5000L,
) : BaseTask(dependsOnTask, weightPercentage = 10) {

    private val decoderJSON = Json {
        ignoreUnknownKeys = true
        serializersModule = serializersModuleOf(Any::class, AnySerializer)
    }

    private var weightProgressSum: Int = 0
    private var totalProgressWeight: Int = 0
    private var progressTask: Int = 0
    private var source: BufferedSource? = null

    /**
     * Executes the task to perform the initial data synchronization.
     *
     * @param previousDataTask Optional data from a previous task. Not used in this task.
     * @return A [TaskResult] indicating success or failure of the task.
     */
    override suspend fun execute(previousDataTask: Any?, weightProgressSum: Int, totalProgressWeight: Int): TaskResult {

        this.weightProgressSum = weightProgressSum
        this.totalProgressWeight = totalProgressWeight
        this.progressTask = 0

        // Check if the initial synchronization has already been completed.
        if (isInitialSynchronizationCompleted()) {
            return TaskResult.success()
        }

        // Check network availability before proceeding.
        if (!networkValidator.isNetworkAvailable()) {
            return TaskResult.failure(Exception("Network is not available"))
        }

        EventBus.emit(EventType.START_SYNCHRONIZATION)

        val dataResult = fetchSyncData()

        // If data retrieval is successful and data is saved, mark the initial synchronization as completed.
        if (dataResult is DataResult.Success) {

            val completionRecords = mutableListOf<Boolean>()

            for (data in dataResult.data) {
                completionRecords.add(saveData(data.toListEntityData()))
            }

            if (completionRecords.all { it }) {
                source?.close()
                completeInitialSynchronization()
                return TaskResult.success()
            }

        }

        source?.close()

        // Return failure if data retrieval or saving fails.
        return TaskResult.failure(Exception("Error synchronizing data"))
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun fetchSyncData(): DataResult<Sequence<List<SyncDTO.Response.Entity>>> {
        val syncId = Uuid.random().toString()
        val requestStartSync = SyncDTO.Request.StartSyncRequest(syncId)

        when (val startResult = synchronizeService.postStartSync(requestStartSync)) {
            is DataResult.Success -> {
                val syncDataUrl = getSyncDataUrl(syncId)
                return synchronizeService.downloadSyncData(syncDataUrl) {
                    emitProgress(it + progressTask, FRAGMENT_PROGRESS_DOWNLOAD_FILE)
                }.fold(
                    onSuccess = {
                        DataResult.Success(streamEntitiesFromFile(it))
                    },
                    onFailure = {
                        return@fold DataResult.Failure(it)
                    }
                )
            }

            is DataResult.Failure -> {
                return startResult
            }

            is DataResult.NotAuthorized -> {
                return startResult
            }

            is DataResult.ClientError -> {
                return startResult
            }
        }
    }

    private suspend fun getSyncDataUrl(syncId: String): String {

        val syncStatus = synchronizeService.getSyncStatus(syncId)

        if (syncStatus is DataResult.Success) {

            if (syncStatus.data.status == "failed") {
                throw IllegalStateException("Synchronization failed with status: ${syncStatus.data.status}")
            }

            syncStatus.data.downloadUrl?.let {
                return it
            }
            progressTask += 1
            emitProgress(progressTask, FRAGMENT_PROGRESS_GENERATE_SYNC_DATA)
            delay(pollingTime)
            return getSyncDataUrl(syncId) // Retry after a delay if no URL is found
        }

        throw IllegalStateException("Failed to retrieve synchronization status")
    }

    /**
     * Saves the retrieved data into the operation database.
     *
     * @param entities List of entities to be saved.
     * @param postCreated Callback function to be called after data is saved.
     * @return True if the data was successfully saved, false otherwise.
     */
    private fun saveData(entities: List<Horus.Entity>): Boolean {
        val operations = entities.sortedBy { controlDatabaseHelper.getEntityLevel(it.name) }.flatMap { it.toRecordsInsert() }
        return operationDatabaseHelper.insertWithTransaction(operations)
    }

    /**
     * Checks if the initial synchronization has been completed.
     *
     * @return True if the initial synchronization is completed, false otherwise.
     */
    private fun isInitialSynchronizationCompleted(): Boolean {
        return controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION)
    }

    /**
     * Marks the initial synchronization as completed.
     */
    private fun completeInitialSynchronization() {
        controlDatabaseHelper.addSyncTypeStatus(
            SyncControl.OperationType.INITIAL_SYNCHRONIZATION,
            SyncControl.Status.COMPLETED
        )
        controlDatabaseHelper.addSyncTypeStatus(
            SyncControl.OperationType.CHECKPOINT,
            SyncControl.Status.COMPLETED
        )
    }

    private fun emitProgress(progress: Int, maxChunk: Int) {
        if (progress > maxChunk) {
            return
        }
        emitProgress(weightProgressSum, totalProgressWeight, progress)
    }

    private fun streamEntitiesFromFile(path: Path): Sequence<List<SyncDTO.Response.Entity>> {
        val fileSystem = FileSystem.SYSTEM
        val totalSize = fileSystem.metadata(path).size ?: 0L
        var readBytes = 0L
        var countLines = 0
        var progressTask = FRAGMENT_PROGRESS_DOWNLOAD_FILE

        source = fileSystem.source(path).buffer()

        return generateSequence {
            val line = source?.readUtf8Line() ?: return@generateSequence null
            val lineBytes = line.toByteArray(Charsets.UTF_8).size.toLong()
            readBytes += lineBytes
            countLines++

            if (countLines % (BATCH_SIZE * 5) == 0) {
                val progress = ((readBytes.toDouble() / totalSize) * 100).toInt()
                info("[SynchronizeInitialDataTask] Reading data... Lines:$countLines Progress: $progress%")
                progressTask += 1
                emitProgress(progressTask, FRAGMENT_PROGRESS_MIGRATE_DATA)
            }

            try {
                decoderJSON.decodeFromString(SyncDTO.Response.Entity.serializer(), line)
            } catch (e: Exception) {
                logException("[SynchronizeInitialDataTask] Error decoding entity from line: $line", e)
                null
            }
        }.chunked(BATCH_SIZE)
    }


    companion object {
        const val FRAGMENT_PROGRESS_GENERATE_SYNC_DATA = 30 // Percentage of progress for generating sync data
        const val FRAGMENT_PROGRESS_DOWNLOAD_FILE = 50 // Progress percentage for downloading the file
        const val FRAGMENT_PROGRESS_MIGRATE_DATA = 99 // Percentage of progress for migrating data
        const val BATCH_SIZE = 1000 // Number of entities to process in each batch
    }
}
