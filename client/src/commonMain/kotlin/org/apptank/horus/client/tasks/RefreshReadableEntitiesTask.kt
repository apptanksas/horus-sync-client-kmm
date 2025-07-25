package org.apptank.horus.client.tasks

import com.russhwolf.settings.Settings
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.database.struct.toRecordsInsert
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.extensions.diffInHoursFromNow
import org.apptank.horus.client.extensions.forEachPair
import org.apptank.horus.client.extensions.logException
import org.apptank.horus.client.sync.network.dto.toEntityData
import org.apptank.horus.client.sync.network.service.ISynchronizationService

/**
 * Task responsible for refreshing all readable entities by fetching their latest
 * data from the synchronization service and updating the local operation store.
 *
 * This task depends on [RetrieveDataSharedTask] to ensure shared data has been
 * retrieved before refreshing individual entities.
 *
 * @property settings                    Provides persistent storage for the last-refresh timestamp.
 * @property networkValidator            Validates current network connectivity.
 * @property syncService                 Service to fetch entity data from the server.
 * @property operationDatabaseHelper     Helper for CRUD operations on per-entity operation tables.
 * @property syncControlDatabaseHelper   Helper for retrieving the list of entities to refresh.
 * @param dependsOnTask                   Previous task that this task depends on.
 */
internal class RefreshReadableEntitiesTask(
    private val settings: Settings,
    private val networkValidator: INetworkValidator,
    private val syncService: ISynchronizationService,
    private val operationDatabaseHelper: IOperationDatabaseHelper,
    private val syncControlDatabaseHelper: ISyncControlDatabaseHelper,
    dependsOnTask: RetrieveDataSharedTask
) : BaseTask(dependsOnTask) {

    /**
     * Executes the refresh process.
     *
     * Steps performed:
     * 1. If offline, abort and return success.
     * 2. Check the time elapsed since the last successful refresh; if within TTL, skip.
     * 3. For each registered readable entity name:
     *    a. Fetch latest data via [syncService.getDataEntity].
     *    b. On success, clear the local operation table for that entity.
     *    c. Map and batch-insert the new entity records in reverse order.
     *    d. On failure or unauthorized, log and skip further processing.
     * 4. Update the last-refresh timestamp.
     *
     * @param previousDataTask  Result or output of the dependent task (unused).
     * @return [TaskResult.success] always, even on no-op or after completion.
     */
    override suspend fun execute(previousDataTask: Any?, weightProgressSum: Int, totalProgressWeight: Int): TaskResult {
        // Skip if offline
        if (networkValidator.isNetworkAvailable().not()) {
            return TaskResult.success()
        }

        val lastDate = getLastRefreshDate()
        val diffInHours = (lastDate?.diffInHoursFromNow() ?: Int.MAX_VALUE)

        // Skip if within TTL window
        if (diffInHours < REFRESH_TTL) {
            return TaskResult.success()
        }

        // Refresh each readable entity
        syncControlDatabaseHelper.getReadableEntityNames().forEach { entityName ->
            when (val response = syncService.getDataEntity(entityName)) {
                is DataResult.Success -> {
                    val entitiesToTruncate = mutableListOf(entityName)
                    val records = response.data
                        .map {
                            it.toEntityData().also {
                                it.relations?.forEachPair { relation, entities ->
                                    entitiesToTruncate.addAll(entities.map { it.name })
                                }
                            }
                        }
                        .flatMap { it.toRecordsInsert() }
                        .reversed()

                    // Remove existing records and insert fresh data
                    entitiesToTruncate.forEach {
                        operationDatabaseHelper.truncate(it)
                    }
                    operationDatabaseHelper.insertWithTransaction(records)
                }

                is DataResult.Failure, is DataResult.NotAuthorized -> {
                    logException("Failed to retrieve data for entity: $entityName")
                    return TaskResult.success()
                }
            }
        }

        updateLastRefreshDate()

        return TaskResult.success()
    }

    /**
     * Retrieves the timestamp of the last successful readable-entities refresh.
     *
     * @return An [Instant] representing the last refresh time, or null if not set.
     */
    private fun getLastRefreshDate(): Instant? =
        settings.getLongOrNull(KEY_LAST_DATE_READABLE_ENTITIES)
            ?.let { Instant.fromEpochSeconds(it) }

    /**
     * Persists the current time as the last successful readable-entities refresh timestamp.
     */
    private fun updateLastRefreshDate() {
        settings.putLong(KEY_LAST_DATE_READABLE_ENTITIES, Clock.System.now().epochSeconds)
    }

    companion object {
        /** Key under which the last refresh timestamp is stored in [Settings]. */
        const val KEY_LAST_DATE_READABLE_ENTITIES = "last_date_readable_entities"

        /** Time-to-live for readable-entities refresh, in hours. */
        const val REFRESH_TTL = 24
    }
}
