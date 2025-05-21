package org.apptank.horus.client.tasks

import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.hashing.AttributeHasher
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.ISynchronizationService
import kotlin.random.Random

/**
 * A task responsible for validating hashing by comparing data hashes with the server.
 *
 * @property controlDatabaseHelper Helper to interact with the sync control database.
 * @property syncService Service to handle synchronization operations.
 * @property dependsOnTask The task that must be completed before this task can run.
 */
internal class ValidateHashingTask(
    private val controlDatabaseHelper: ISyncControlDatabaseHelper,
    private val syncService: ISynchronizationService,
    dependsOnTask: ValidateMigrationLocalDatabaseTask
) : BaseTask(dependsOnTask) {

    /**
     * Executes the task to validate hashing with the server.
     *
     * @param previousDataTask Optional data from a previous task. Not used in this task.
     * @return A [TaskResult] indicating success or failure of the task.
     */
    override suspend fun execute(previousDataTask: Any?): TaskResult {
        // Check if the hashing validation has already been completed.
        if (isValidationHashingCompleted()) {
            return TaskResult.success()
        }

        // Create dummy data for hashing validation.
        val dataToValidate = mutableMapOf<String, Any?>(
            "zattr" to "abc" + Random.nextInt().toString(),
            "id" to Random.nextInt(),
            "name" to Random.nextBytes(10).toString(),
            "float" to 1234.40,
            "float2" to 54.0f,
            "null" to null,
            "boolean" to true,
            "boolean2" to false,
        )

        // Generate a hash of the dummy data.
        val hash = AttributeHasher.generateHash(
            dataToValidate.map { Horus.Attribute(it.key, it.value) }
        )

        // Validate the hash with the server.
        val result = syncService.postValidateHashing(
            SyncDTO.Request.ValidateHashingRequest(
                dataToValidate,
                hash
            )
        )

        return when (result) {
            is DataResult.Success -> {
                // Check if the hash validation matched.
                if (result.data.matched == true) {
                    completeHashingValidation()
                    TaskResult.success()
                } else {
                    TaskResult.failure(Exception("Hashing validation is unmatched"))
                }
            }

            // Handle failure or authorization errors.
            is DataResult.Failure, is DataResult.NotAuthorized -> {
                TaskResult.failure(Exception("Hashing validation failed"))
            }
        }
    }

    /**
     * Checks if the hashing validation has been completed.
     *
     * @return True if the hashing validation is completed, false otherwise.
     */
    private fun isValidationHashingCompleted(): Boolean {
        return controlDatabaseHelper.isStatusCompleted(SyncControl.OperationType.HASH_VALIDATION)
    }

    /**
     * Marks the hashing validation as completed.
     */
    private fun completeHashingValidation() {
        controlDatabaseHelper.addSyncTypeStatus(
            SyncControl.OperationType.HASH_VALIDATION,
            SyncControl.Status.COMPLETED
        )
    }
}
