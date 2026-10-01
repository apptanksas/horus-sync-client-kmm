package org.apptank.horus.client.tasks

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.sync.manager.PushDataRemoteSynchronizatorManager
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
    private val pushDataRemoteSynchronizatorManager: PushDataRemoteSynchronizatorManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    dependsOnTask: SynchronizeInitialDataTask
) : BaseTask(dependsOnTask) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

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
        val deferred = CompletableDeferred<TaskResult>()

        // Start the synchronization process and update the statusResult based on completion.
        manager.start { status, isCompleted ->

            if (isCompleted) {
                statusResult = status
            }

            // Success
            if (statusResult == SyncStatus.SUCCESS || statusResult == SyncStatus.IDLE) {
                val job: Job
                job = scope.launch {
                    pushDataRemoteSynchronizatorManager.tryPushData()
                    deferred.complete(TaskResult.success())
                }
                job.invokeOnCompletion {
                    job.cancel()
                    it?.let {
                        deferred.complete(TaskResult.failure(Exception("Error synchronizing data")))
                    }
                }
            }

            if (statusResult == SyncStatus.FAILED) {
                deferred.complete(TaskResult.failure(Exception("Error synchronizing data")))
            }
        }

        return deferred.await()
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
