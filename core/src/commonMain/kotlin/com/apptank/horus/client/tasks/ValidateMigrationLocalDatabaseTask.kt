package com.apptank.horus.client.tasks

import app.cash.sqldelight.db.AfterVersion
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.database.HorusDatabase
import com.apptank.horus.client.migration.database.DatabaseTablesCreatorDelegate
import com.apptank.horus.client.migration.database.DatabaseUpgradeDelegate
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.domain.getLastVersion
import com.russhwolf.settings.Settings


class ValidateMigrationLocalDatabaseTask(
    private val settings: Settings,
    private val databaseDriverFactory: IDatabaseDriverFactory,
    private val syncControlDatabase: ISyncControlDatabaseHelper,
    dependsOnTask: RetrieveDatabaseSchemeTask
) : BaseTask(dependsOnTask) {

    override suspend fun execute(previousDataTask: Any?): TaskResult {

        val schema = getDatabaseSchema()
        val data = (previousDataTask as? List<EntityScheme>?)
            ?: return TaskResult.failure(Exception("Invalid data"))

        return runCatching {
            val lastVersion = getLastVersion(data)
            val schemaVersion = getCurrentSchemaVersion()

            // Create database schema if it doesn't exist
            if (schemaVersion == null) {
                schema.create(databaseDriverFactory.createDriver(), data)
                syncControlDatabase.createControlTablesIfNotExists()
                setSchemaVersion(schema.version)
                return TaskResult.success()
            }

            // Migrate database schema to the last version
            if (lastVersion > schemaVersion) {
                schema.migrate(
                    databaseDriverFactory.createDriver(),
                    schemaVersion,
                    lastVersion,
                    AfterVersion(lastVersion) {
                        setSchemaVersion(lastVersion)
                    }
                )
                return TaskResult.success()
            }

            return TaskResult.success()

        }.getOrElse {
            TaskResult.failure(it)
        }
    }

    private fun getDatabaseSchema() = databaseDriverFactory.getSchema()

    private fun getLastVersion(entities: List<EntityScheme>): Long {
        return entities.getLastVersion()
    }

    private fun getCurrentSchemaVersion(): Long? {
        return settings.getLongOrNull(SCHEMA_VERSION_KEY)
    }

    private fun setSchemaVersion(version: Long) {
        settings.putLong(SCHEMA_VERSION_KEY, version)
    }

    companion object {
        const val SCHEMA_VERSION_KEY = "horus_db_schema_version"
    }

}