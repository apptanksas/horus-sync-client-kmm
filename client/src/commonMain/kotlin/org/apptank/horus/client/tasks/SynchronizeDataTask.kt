package org.apptank.horus.client.tasks

import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.sync.manager.SynchronizatorManager
import org.apptank.horus.client.sync.manager.SynchronizatorManager.SynchronizationStatus as SyncStatus
import org.apptank.horus.client.sync.network.service.ISynchronizationService

/**
 * A task responsible for synchronizing data using various services.
 *
 * @property netWorkValidator Validator to check network connectivity.
 * @property syncControlDatabaseHelper Helper to interact with the sync control database.
 * @property operationDatabaseHelper Helper to interact with the operation database.
 * @property synchronizationService Service to handle synchronization operations.
 * @property dependsOnTask The task that must be completed before this task can run.
 */
internal class SynchronizeDataTask(
    private val netWorkValidator: INetworkValidator,
    private val syncControlDatabaseHelper: ISyncControlDatabaseHelper,
    private val operationDatabaseHelper: IOperationDatabaseHelper,
    private val synchronizationService: ISynchronizationService,
    dependsOnTask: SynchronizeInitialDataTask
) : BaseTask(dependsOnTask) {

    /**
     * Executes the task to synchronize data.
     *
     * @param previousDataTask Optional data from a previous task. Not used in this task.
     * @return A [TaskResult] indicating success or failure of the task.
     */
    override suspend fun execute(previousDataTask: Any?, weightProgressSum: Int, totalProgressWeight: Int): TaskResult {
        // Create a manager for data validation and synchronization.
        val manager = createDataValidatorManager()

        // Variable to hold the synchronization status.
        var statusResult: SyncStatus = SyncStatus.IN_PROGRESS

        // Start the synchronization process and update the statusResult based on completion.
        manager.start { status, isCompleted ->
            if (isCompleted) {
                statusResult = status
            }
        }

        // Return success if the synchronization was successful or idle, otherwise return failure.
        return if (statusResult == SyncStatus.SUCCESS || statusResult == SyncStatus.IDLE) {
            TaskResult.success()
        } else {
            TaskResult.failure(Exception("Error synchronizing data"))
        }
    }

    /**
     * Creates an instance of [SynchronizatorManager] with the necessary dependencies.
     *
     * @return A new instance of [SynchronizatorManager].
     */
    private fun createDataValidatorManager(): SynchronizatorManager {
        return SynchronizatorManager(
            netWorkValidator,
            syncControlDatabaseHelper,
            operationDatabaseHelper,
            synchronizationService
        )
    }
}
