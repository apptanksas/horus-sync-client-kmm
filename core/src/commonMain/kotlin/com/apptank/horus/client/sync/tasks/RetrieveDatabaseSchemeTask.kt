package com.apptank.horus.client.sync.tasks

import com.apptank.horus.client.base.fold
import com.apptank.horus.client.migration.network.service.IMigrationService
import com.apptank.horus.client.migration.network.toScheme
import com.apptank.horus.client.sync.tasks.base.BaseTask
import com.apptank.horus.client.sync.tasks.base.TaskResult

class RetrieveDatabaseSchemeTask(
    private val migrationService: IMigrationService
) : BaseTask() {

    override suspend fun execute(previousDataTask: Any?): TaskResult {
        return migrationService.getMigration().fold(
            onSuccess = {
                TaskResult.success(it.map { it.toScheme() })
            },
            onFailure = {
                TaskResult.failure(it)
            })
    }


}