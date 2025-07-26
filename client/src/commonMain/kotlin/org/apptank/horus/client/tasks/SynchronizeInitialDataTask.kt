package org.apptank.horus.client.tasks

import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.fold
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.database.struct.toRecordsInsert
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
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
        if (dataResult is DataResult.Success && saveData(dataResult.data.toListEntityData()) {
                // Complete initial synchronization when data is saved
                completeInitialSynchronization()
            }) {
            return TaskResult.success()
        }

        // Return failure if data retrieval or saving fails.
        return TaskResult.failure(Exception("Error synchronizing data"))
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun fetchSyncData(): DataResult<List<SyncDTO.Response.Entity>> {
        val syncId = Uuid.random().toString()
        val requestStartSync = SyncDTO.Request.StartSyncRequest(syncId)

        when (val startResult = synchronizeService.postStartSync(requestStartSync)) {
            is DataResult.Success -> {
                val syncDataUrl = getSyncDataUrl(syncId)
                return synchronizeService.downloadSyncData(syncDataUrl) {
                    emitProgress(it + progressTask)
                }.fold(
                    onSuccess = {
                        DataResult.Success(
                            decoderJSON.decodeFromString(it.data.decodeToString())
                        )
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
        }
    }


    private suspend fun getSyncDataUrl(syncId: String): String {

        val syncStatus = synchronizeService.getSyncStatus(syncId)

        if (syncStatus is DataResult.Success) {
            syncStatus.data.downloadUrl?.let {
                return it
            }
            progressTask += 1
            emitProgress(progressTask)
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
    private fun saveData(entities: List<Horus.Entity>, postCreated: Callback): Boolean {
        val operations = entities.flatMap { it.toRecordsInsert() }.reversed()
        return operationDatabaseHelper.insertWithTransaction(operations) {
            postCreated()
        }
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

    private fun emitProgress(progress: Int) {
        if (progress > LIMIT_PROGRESS_MAX) {
            return
        }
        emitProgress(weightProgressSum, totalProgressWeight, progress)
    }

    companion object {
        const val LIMIT_PROGRESS_MAX = 95 // Maximum progress limit to leave room for final processing
    }
}
