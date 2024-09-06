package com.apptank.horus.client.sync.tasks

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.hashing.AttributeHasher
import com.apptank.horus.client.sync.network.dto.SyncDTO
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import kotlin.random.Random

class ValidateHashingTask(
    private val controlDatabaseHelper: ISyncControlDatabaseHelper,
    private val syncService: ISynchronizationService
) : BaseTask() {

    override suspend fun execute(previousDataTask: Any?): TaskResult {

        if (isValidationHashingCompleted()) {
            return TaskResult.success()
        }

        // This is a dummy data to validate the hashing with the server
        val dataToValidate = mutableMapOf<String, Any>(
            "zattr" to Random.toString(),
            "id" to Random.nextInt(),
            "name" to Random.nextBytes(10).toString()
        )

        val hash = AttributeHasher.generateHash(
            dataToValidate.map { Horus.Attribute(it.key, it.value) }
        )

        val result = syncService.postValidateHashing(
            SyncDTO.Request.ValidateHashingRequest(
                dataToValidate,
                hash
            )
        )

        when (result) {
            is DataResult.Success -> {
                if (result.data.matched == true) {
                    completeHashingValidation()
                    return TaskResult.success()
                }
                return TaskResult.failure(Exception("Hashing validation is unmatched"))
            }

            is DataResult.Failure, is DataResult.NotAuthorized -> {
                return TaskResult.failure(Exception("Hashing validation failed"))
            }
        }
    }

    private fun isValidationHashingCompleted(): Boolean {
        return controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASHING_VALIDATED)
    }

    private fun completeHashingValidation() {
        controlDatabaseHelper.addSyncTypeStatus(
            SyncControl.OperationType.HASHING_VALIDATED,
            SyncControl.Status.COMPLETED
        )
    }
}