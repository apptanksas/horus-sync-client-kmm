package org.apptank.horus.client.sync.manager

import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.coFold
import org.apptank.horus.client.control.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.eventbus.Event
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.log
import org.apptank.horus.client.extensions.logException
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.extensions.warn
import org.apptank.horus.client.sync.network.dto.toRequest
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Manages the synchronization of data with a remote server, handling network changes, and retrying failed attempts.
 *
 * This class is responsible for monitoring network availability, attempting data synchronization when network
 * connectivity is restored, and managing retries for synchronization operations. It utilizes a background coroutine
 * dispatcher for performing network and database operations.
 *
 * @param netWorkValidator An instance of `INetworkValidator` to monitor network availability.
 * @param syncControlDatabaseHelper An instance of `ISyncControlDatabaseHelper` for accessing and updating the local sync control database.
 * @param synchronizationService An instance of `ISynchronizationService` for posting queued actions to the remote server.
 * @param event An instance of `EventBus` for emitting synchronization events.
 * @param dispatcher A coroutine dispatcher for background operations (default is `Dispatchers.IO`).
 * @param maxAttempts The maximum number of retry attempts for synchronization operations (default is 3).
 */
internal class RemoteSynchronizatorManager(
    private val netWorkValidator: INetworkValidator,
    private val syncControlDatabaseHelper: ISyncControlDatabaseHelper,
    private val synchronizationService: ISynchronizationService,
    private val event: EventBus = EventBus,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxAttempts: Int = 3
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    init {
        // Registers a callback to attempt synchronization when network changes are detected.
        netWorkValidator.onNetworkChange {
            trySynchronizeData()
        }
    }

    /**
     * Attempts to synchronize pending data with the remote server.
     *
     * This method checks for network availability and initiates synchronization if there are pending actions in the local
     * sync control database. It handles retry logic and error reporting for synchronization operations.
     */
    fun trySynchronizeData() {

        if (HorusAuthentication.isNotUserAuthenticated()) {
            warn("User is not authenticated")
            return
        }

        if (!netWorkValidator.isNetworkAvailable()) {
            info("No network available")
            return
        }

        if (isTakenProcess) {
            warn("Process synchronizator already in progress")
            return
        }

        scope.apply {
            val job = launch {

                takeProcess()

                val pendingActions = syncControlDatabaseHelper.getPendingActions()

                if (pendingActions.isEmpty()) {
                    return@launch
                }

                attemptOperationResult {
                    synchronizationService.postQueueActions(pendingActions.map { it.toRequest() })
                }.coFold(
                    onSuccess = {
                        updateActionsAsCompleted(pendingActions)
                    },
                    onFailure = {
                        logException("Error trying to sync actions")
                        event.emit(
                            EventType.SYNC_PUSH_FAILED,
                            Event(mapOf<String, Any>("exception" to it))
                        )
                    })
            }

            job.invokeOnCompletion {
                it?.let {
                    logException("Error trying to sync actions", it)
                    event.emit(
                        EventType.SYNC_PUSH_FAILED,
                        Event(mapOf<String, Any>("exception" to it))
                    )
                }
                job.cancel()
                releaseProcess()
            }
        }
    }

    /**
     * Updates the status of synchronized actions in the local sync control database.
     *
     * This method marks actions as completed in the database and emits a success or failure event based on the result.
     *
     * @param pendingActions A list of actions that were attempted to be synchronized.
     */
    private suspend fun updateActionsAsCompleted(pendingActions: List<SyncControl.Action>) {

        val actionsId = pendingActions.map { it.id }
        val countInsertedActions =
            pendingActions.filter { it.action == SyncControl.ActionType.INSERT }.size
        val countUpdatedActions =
            pendingActions.filter { it.action == SyncControl.ActionType.UPDATE }.size
        val countDeletedActions =
            pendingActions.filter { it.action == SyncControl.ActionType.DELETE }.size

        if (attemptOperation { syncControlDatabaseHelper.completeActions(actionsId) }) {
            log("[ActionSync] ${actionsId.size} actions were synced. [Inserts: $countInsertedActions, Updates: $countUpdatedActions, Deletes: $countDeletedActions]")
            event.emit(EventType.SYNC_PUSH_SUCCESS)
            return
        }

        event.emit(EventType.SYNC_PUSH_FAILED)
    }

    /**
     * Attempts to execute an operation with retry logic.
     *
     * This method retries the operation up to a maximum number of attempts if it fails. It includes a delay between retries.
     *
     * @param callback A suspending function that performs the operation.
     * @return `true` if the operation was successful, `false` otherwise.
     */
    private suspend fun attemptOperation(callback: () -> Boolean): Boolean {
        var attempts = 0L
        var isSuccess = false
        do {
            isSuccess = callback()
            if (attempts > 0) {
                delay(2000 * attempts)
            }
            attempts++
        } while (!isSuccess && attempts < maxAttempts)

        return isSuccess
    }

    /**
     * Attempts to execute an operation that returns a `DataResult` with retry logic.
     *
     * This method retries the operation up to a maximum number of attempts if it returns a failure result. It includes a delay between retries.
     *
     * @param callback A suspending function that performs the operation and returns a `DataResult`.
     * @return The result of the operation, which may be a success or failure.
     */
    private suspend fun <T> attemptOperationResult(
        callback: suspend () -> DataResult<T>
    ): DataResult<T> {
        var attempts = 0L
        var result: DataResult<T>
        do {
            result = callback()
            if (attempts > 0) {
                delay(2000 * attempts)
            }
            attempts++
        } while (result is DataResult.Failure && attempts < maxAttempts)

        return result
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
