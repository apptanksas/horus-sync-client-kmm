package com.apptank.horus.client.sync.manager

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.base.coFold
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.eventbus.Event
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import com.apptank.horus.client.extensions.info
import com.apptank.horus.client.extensions.log
import com.apptank.horus.client.extensions.logException
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.sync.network.dto.toRequest
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RemoteSynchronizatorManager(
    private val netWorkValidator: INetworkValidator,
    private val syncControlDatabaseHelper: ISyncControlDatabaseHelper,
    private val synchronizationService: ISynchronizationService,
    private val event: EventBus,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxAttempts: Int = 3
) {

    init {
        netWorkValidator.onNetworkChange {
            trySynchronizeData()
        }
    }

    fun trySynchronizeData() {

        if (!netWorkValidator.isNetworkAvailable()) {
            info("No network available")
            return
        }

        CoroutineScope(dispatcher).launch {

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
                    event.post(
                        EventType.SYNC_PUSH_FAILED,
                        Event(mapOf<String, Any>("exception" to it))
                    )
                })
        }.invokeOnCompletion {
            it?.let {
                logException("Error trying to sync actions", it)
                event.post(EventType.SYNC_PUSH_FAILED, Event(mapOf<String, Any>("exception" to it)))
            }
        }
    }


    private suspend fun updateActionsAsCompleted(pendingActions: List<SyncControl.Action>) {

        val actionsId = pendingActions.map { it.id }
        val countInsertedActions = pendingActions.filter { it.action == SyncControl.ActionType.INSERT }.size
        val countUpdatedActions = pendingActions.filter { it.action == SyncControl.ActionType.UPDATE }.size
        val countDeletedActions = pendingActions.filter { it.action == SyncControl.ActionType.DELETE }.size

        if (attemptOperation { syncControlDatabaseHelper.completeActions(actionsId) }) {
            log("[ActionSync] ${actionsId.size} actions were synced. [Inserts: $countInsertedActions, Updates: $countUpdatedActions, Deletes: $countDeletedActions]")
            event.post(EventType.SYNC_PUSH_SUCCESS)
            return
        }

        event.post(EventType.SYNC_PUSH_FAILED)
    }

    private suspend fun attemptOperation(callback: () -> Boolean): Boolean {
        var attempts = 0
        var isSuccess = false
        do {
            isSuccess = callback()
            if (attempts > 0) {
                delay(1000)
            }
            attempts++
        } while (!isSuccess && attempts < maxAttempts)

        return isSuccess
    }

    private suspend fun <T> attemptOperationResult(
        callback: suspend () -> DataResult<T>
    ): DataResult<T> {
        var attempts = 0
        var result: DataResult<T>
        do {
            result = callback()
            if (attempts > 0) {
                delay(1000)
            }
            attempts++
        } while (result is DataResult.Failure && attempts < maxAttempts)

        return result
    }

}