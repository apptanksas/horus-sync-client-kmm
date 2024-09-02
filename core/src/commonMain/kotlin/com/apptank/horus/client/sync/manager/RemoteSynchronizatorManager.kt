package com.apptank.horus.client.sync.manager

import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.base.coFold
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncAction
import com.apptank.horus.client.control.SyncActionType
import com.apptank.horus.client.control.SyncControlDatabaseHelper
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
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
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
                    logException("Error trying to sync actions", it)
                })
        }
    }


    private suspend fun updateActionsAsCompleted(pendingActions: List<SyncAction>) {

        val actionsId = pendingActions.map { it.id }
        val countInsertedActions = pendingActions.filter { it.action == SyncActionType.INSERT }.size
        val countUpdatedActions = pendingActions.filter { it.action == SyncActionType.UPDATE }.size
        val countDeletedActions = pendingActions.filter { it.action == SyncActionType.DELETE }.size

        if (attemptOperation { syncControlDatabaseHelper.completeActions(actionsId) }) {
            log("[ActionSync] ${actionsId.size} actions were synced. [Inserts: $countInsertedActions, Updates: $countUpdatedActions, Deletes: $countDeletedActions]")
            event.post(EventType.SYNC_PUSH_SUCCESS)
            return
        }

        event.post(EventType.SYNC_PUSH_FAILED)
    }

    private suspend fun attemptOperation(maxAttempts: Int = 3, callback: () -> Boolean): Boolean {
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
        maxAttempts: Int = 3,
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