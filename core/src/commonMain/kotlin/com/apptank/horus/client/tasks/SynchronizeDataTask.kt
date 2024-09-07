package com.apptank.horus.client.tasks

import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.database.IOperationDatabaseHelper
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.sync.manager.SynchronizatorManager
import com.apptank.horus.client.sync.manager.SynchronizatorManager.SynchronizationStatus as SyncStatus
import com.apptank.horus.client.sync.network.service.ISynchronizationService

class SynchronizeDataTask(
    private val netWorkValidator: INetworkValidator,
    private val syncControlDatabaseHelper: ISyncControlDatabaseHelper,
    private val operationDatabaseHelper: IOperationDatabaseHelper,
    private val synchronizationService: ISynchronizationService,
    dependsOnTask: SynchronizeInitialDataTask
) : BaseTask(dependsOnTask) {

    override suspend fun execute(previousDataTask: Any?): TaskResult {
        val manager = createDataValidatorManager()

        var statusResult: SyncStatus = SyncStatus.IN_PROGRESS

        manager.start { status, isCompleted ->
            if (isCompleted) {
                statusResult = status
            }
        }

        return if (statusResult == SyncStatus.SUCCESS || statusResult == SyncStatus.IDLE) {
            TaskResult.success()
        } else {
            TaskResult.failure(Exception("Error synchronizing data"))
        }
    }

    private fun createDataValidatorManager(): SynchronizatorManager {
        return SynchronizatorManager(
            netWorkValidator,
            syncControlDatabaseHelper,
            operationDatabaseHelper,
            synchronizationService
        )
    }

}