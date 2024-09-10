package com.apptank.horus.client.tasks

import app.cash.sqldelight.db.AfterVersion
import com.apptank.horus.client.interfaces.IDatabaseDriverFactory
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.domain.getLastVersion
import com.russhwolf.settings.Settings

/**
 * A task responsible for validating and migrating the local database schema.
 *
 * @property settings Provides access to application settings, including schema version.
 * @property databaseDriverFactory Factory to create database drivers for schema operations.
 * @property dependsOnTask The task that must be completed before this task can run.
 */
internal class ValidateMigrationLocalDatabaseTask(
    private val settings: Settings,
    private val databaseDriverFactory: IDatabaseDriverFactory,
    dependsOnTask: RetrieveDatabaseSchemeTask
) : BaseTask(dependsOnTask) {

    /**
     * Executes the task to validate and migrate the local database schema.
     *
     * @param previousDataTask Data from a previous task. Expected to be a list of [EntityScheme].
     * @return A [TaskResult] indicating success or failure of the task.
     */
    override suspend fun execute(previousDataTask: Any?): TaskResult {

        // Retrieve the database schema.
        val schema = getDatabaseSchema()

        // Ensure previous data is of the expected type (List<EntityScheme>).
        val data = (previousDataTask as? List<EntityScheme>?)
            ?: return TaskResult.failure(Exception("Invalid data"))

        return runCatching {
            // Get the last version of the schema.
            val lastVersion = getLastVersion(data)
            // Get the current schema version from settings.
            val schemaVersion = getCurrentSchemaVersion()

            // Create database schema if it doesn't exist.
            if (schemaVersion == null) {
                schema.create(databaseDriverFactory.getDriver(), data)
                setSchemaVersion(schema.version)
                return TaskResult.success()
            }

            // Migrate database schema to the last version if needed.
            if (lastVersion > schemaVersion) {
                schema.migrate(
                    databaseDriverFactory.getDriver(),
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

    /**
     * Retrieves the database schema from the database driver factory.
     *
     * @return The [DatabaseSchema] instance.
     */
    private fun getDatabaseSchema() = databaseDriverFactory.getSchema()

    /**
     * Retrieves the last version of the database schema from the list of entity schemes.
     *
     * @param entities List of [EntityScheme] representing the database schema.
     * @return The last version of the schema.
     */
    private fun getLastVersion(entities: List<EntityScheme>): Long {
        return entities.getLastVersion()
    }

    /**
     * Retrieves the current schema version from the settings.
     *
     * @return The current schema version, or null if not set.
     */
    private fun getCurrentSchemaVersion(): Long? {
        return settings.getLongOrNull(SCHEMA_VERSION_KEY)
    }

    /**
     * Sets the schema version in the settings.
     *
     * @param version The version to be set.
     */
    private fun setSchemaVersion(version: Long) {
        settings.putLong(SCHEMA_VERSION_KEY, version)
    }

    companion object {
        // Key used to store the schema version in settings.
        const val SCHEMA_VERSION_KEY = "horus_db_schema_version"
    }
}
