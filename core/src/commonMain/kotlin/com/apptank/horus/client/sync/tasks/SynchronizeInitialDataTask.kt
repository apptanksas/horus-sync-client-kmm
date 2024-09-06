package com.apptank.horus.client.sync.tasks

import com.apptank.horus.client.base.Callback
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.database.IOperationDatabaseHelper
import com.apptank.horus.client.database.toRecordsInsert
import com.apptank.horus.client.sync.network.dto.toListEntityData
import com.apptank.horus.client.sync.network.service.ISynchronizationService

class SynchronizeInitialDataTask(
    private val operationDatabaseHelper: IOperationDatabaseHelper,
    private val controlDatabaseHelper: ISyncControlDatabaseHelper,
    private val synchronizeService: ISynchronizationService,
     dependsOnTask: ValidateMigrationLocalDatabaseTask
) : BaseTask(dependsOnTask) {

    override suspend fun execute(previousDataTask: Any?): TaskResult {

        if (isInitialSynchronizationCompleted()) {
            return TaskResult.success()
        }

        val dataResult = synchronizeService.getData()

        if (dataResult is DataResult.Success && saveData(dataResult.data.toListEntityData()) {
                // Complete initial synchronization when data is saved
                completeInitialSynchronization()
            }) {
            return TaskResult.success()
        }

        return TaskResult.failure(Exception("Error synchronizing data"))
    }

    private fun saveData(entities: List<Horus.Entity>, postCreated: Callback): Boolean {
        val operations = entities.flatMap { it.toRecordsInsert() }
        return operationDatabaseHelper.insertWithTransaction(operations) {
            postCreated()
        }
    }

    private fun isInitialSynchronizationCompleted(): Boolean {
        return controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.INITIAL_SYNCHRONIZATION)
    }

    private fun completeInitialSynchronization() {
        controlDatabaseHelper.addSyncTypeStatus(
            SyncControl.OperationType.INITIAL_SYNCHRONIZATION,
            SyncControl.Status.COMPLETED
        )
    }
}