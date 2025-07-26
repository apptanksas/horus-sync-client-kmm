package org.apptank.horus.client.tasks

import org.apptank.horus.client.base.fold
import org.apptank.horus.client.migration.network.service.IMigrationService
import org.apptank.horus.client.migration.network.toScheme

/**
 * A task responsible for retrieving the database schema from the migration service.
 *
 * @property migrationService The service used to get migration data.
 */
internal class RetrieveDatabaseSchemeTask(
    private val migrationService: IMigrationService
) : BaseTask() {

    /**
     * Executes the task to retrieve the database schema.
     *
     * @param previousDataTask Optional data from a previous task, not used in this task.
     * @return A [TaskResult] indicating success or failure of the task.
     */
    override suspend fun execute(previousDataTask: Any?, weightProgressSum: Int, totalProgressWeight: Int): TaskResult {
        return migrationService.getMigration().fold(
            onSuccess = {
                // On success, map the migration data to the schema and return it.
                TaskResult.success(it?.map { it.toScheme() })
            },
            onFailure = {
                // On failure, return the error.
                TaskResult.failure(it)
            }
        )
    }
}
