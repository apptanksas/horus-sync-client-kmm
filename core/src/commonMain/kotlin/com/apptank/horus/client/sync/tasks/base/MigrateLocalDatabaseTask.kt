package com.apptank.horus.client.sync.tasks.base

import com.apptank.horus.client.di.HorusContainer.getDatabaseFactory
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.migration.database.DatabaseSchema
import com.apptank.horus.client.migration.database.DatabaseTablesCreatorDelegate
import com.apptank.horus.client.migration.database.DatabaseUpgradeDelegate
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.sync.tasks.RetrieveDatabaseSchemeTask


class MigrateLocalDatabaseTask(
    private val databaseDriverFactory: IDatabaseDriverFactory,
    dependsOnTask: RetrieveDatabaseSchemeTask
) : BaseTask(dependsOnTask) {

    override suspend fun execute(previousDataTask: Any?): TaskResult {
        val data = previousDataTask as? List<EntityScheme>
            ?: return TaskResult.failure(Exception("Invalid data"))

        val databaseSchema = createDatabaseScheme(data)
        databaseSchema.create(databaseDriverFactory.createDriver())
        return TaskResult.success()
    }

    private fun createDatabaseScheme(entities: List<EntityScheme>): DatabaseSchema {
        return DatabaseSchema(
            getDatabaseFactory().getDatabaseName(),
            getDatabaseFactory().createDriver(),
            1,
            DatabaseTablesCreatorDelegate(entities),
            DatabaseUpgradeDelegate(entities)
        )
    }

}