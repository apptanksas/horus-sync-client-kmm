package org.apptank.horus.client.sync.manager

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.bus.HorusClientQueueActionReceivedEventBus
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.database.struct.toDeleteRecord
import org.apptank.horus.client.database.struct.toInsertRecord
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.warn
import org.apptank.horus.client.websocket.RealtimeSyncEventsSubscriber

internal class RealtimeSynchronizatorManager(
    private val netWorkValidator: INetworkValidator,
    private val syncControlDatabaseHelper: ISyncControlDatabaseHelper,
    private val realtimeSyncEventsSubscriber: RealtimeSyncEventsSubscriber,
    operationDatabaseHelper: IOperationDatabaseHelper,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseSynchronizator(operationDatabaseHelper) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var coroutineJob: Job? = null

    init {
        netWorkValidator.onNetworkChange {
            start()
        }
    }

    fun start() {

        if (netWorkValidator.isNetworkAvailable().not()) {
            cancelCoroutineJob()
            return
        }

        if (HorusAuthentication.isNotUserAuthenticated()) {
            cancelCoroutineJob()
            return
        }

        if (HorusAuthentication.getEffectiveUserId() != HorusAuthentication.getUserAuthenticatedId()) {
            cancelCoroutineJob()
            info("[RealtimeSynchronizatorManager] Refreshing user authenticated id")
        }

        if (coroutineJob?.isActive == true) {
            info("[RealtimeSynchronizatorManager] Subscription already active")
            return
        }

        coroutineJob = scope.launch {

            realtimeSyncEventsSubscriber.subscriber(HorusAuthentication.getEffectiveUserId()) { action ->

                val eventId: String = action.eventId ?: return@subscriber
                val isEventAlreadyProcessed = syncControlDatabaseHelper.getExistsActionEventIds(listOf(eventId))[eventId] ?: false

                if (isEventAlreadyProcessed) {
                    info("[RealtimeSynchronizatorManager] Event action already processed: $action")
                    return@subscriber
                }

                runCatching {

                    val databaseOperation = when (action.action) {
                        SyncControl.ActionType.INSERT -> action.toInsertRecord(getUserId())
                        SyncControl.ActionType.UPDATE -> mapActionToUpdateDatabaseOperation(action)
                        SyncControl.ActionType.DELETE -> action.toDeleteRecord()
                        SyncControl.ActionType.MOVE -> return@subscriber
                    } ?: return@subscriber

                    val result = operationDatabaseHelper.executeOperations(listOf(databaseOperation)) {
                        syncControlDatabaseHelper.addActionsCompleted(listOf(action))
                    }

                    if (result) {
                        HorusClientQueueActionReceivedEventBus.emit(action)
                        info("[RealtimeSynchronizatorManager] Event action processed: $action")
                    } else {
                        warn("[RealtimeSynchronizatorManager] Error processing event action: ${action.action}")
                    }
                }.getOrElse { exception ->
                    warn("[RealtimeSynchronizatorManager] Error processing event action [EventID: ${action.eventId}]: ${exception.message}")
                }
            }
        }
    }

    private fun cancelCoroutineJob() {
        if (coroutineJob?.isActive == true) {
            coroutineJob?.cancel()
            coroutineJob = null
        }
    }


}