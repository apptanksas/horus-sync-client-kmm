package org.apptank.horus.client.tasks

import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.IOperationDatabaseHelper
import org.apptank.horus.client.database.toRecordsInsert
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.sync.network.dto.toListEntityData
import org.apptank.horus.client.sync.network.service.ISynchronizationService

/**
 * A task responsible for performing the initial data synchronization.
 *
 * @property networkValidator Validator to check network connectivity.
 * @property operationDatabaseHelper Helper to interact with the operation database.
 * @property controlDatabaseHelper Helper to interact with the sync control database.
 * @property synchronizeService Service to handle synchronization operations.
 * @property dependsOnTask The task that must be completed before this task can run.
 */
internal class SynchronizeInitialDataTask(
    private val networkValidator: INetworkValidator,
    private val operationDatabaseHelper: IOperationDatabaseHelper,
    private val controlDatabaseHelper: ISyncControlDatabaseHelper,
    private val synchronizeService: ISynchronizationService,
    dependsOnTask: ValidateHashingTask
) : BaseTask(dependsOnTask) {

    /**
     * Executes the task to perform the initial data synchronization.
     *
     * @param previousDataTask Optional data from a previous task. Not used in this task.
     * @return A [TaskResult] indicating success or failure of the task.
     */
    override suspend fun execute(previousDataTask: Any?): TaskResult {
        // Check if the initial synchronization has already been completed.
        if (isInitialSynchronizationCompleted()) {
            return TaskResult.success()
        }

        // Check network availability before proceeding.
        if (!networkValidator.isNetworkAvailable()) {
            return TaskResult.failure(Exception("Network is not available"))
        }

        // Fetch data from the synchronization service.
        val dataResult = synchronizeService.getData()

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
}
