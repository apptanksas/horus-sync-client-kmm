package org.apptank.horus.client.tasks

import com.russhwolf.settings.Settings
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.IDataSharedDatabaseHelper
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.extensions.diffInHoursFromNow
import org.apptank.horus.client.sync.network.service.ISynchronizationService

/**
 * Task responsible for retrieving shared data from the synchronization service
 * and storing it locally, respecting network availability and a time-to-live (TTL).
 *
 * This task depends on [SynchronizeInitialDataTask] to ensure initial data sync
 * has completed before fetching shared data.
 *
 * @property settings            Provides persistent storage for last-download timestamp.
 * @property networkValidator    Validates current network connectivity.
 * @property databaseHelper      Helper for CRUD operations on the shared-data database table.
 * @property syncService         Service to fetch shared data from the server.
 * @param dependsOnTask          Previous task that this task depends on.
 */
internal class RetrieveDataSharedTask(
    private val settings: Settings,
    private val networkValidator: INetworkValidator,
    private val databaseHelper: IDataSharedDatabaseHelper,
    private val syncService: ISynchronizationService,
    dependsOnTask: SynchronizeDataTask
) : BaseTask(dependsOnTask) {

    /**
     * Executes the retrieval process.
     *
     * Steps performed:
     * 1. If no network is available, abort and return success.
     * 2. Check the time elapsed since the last successful download; if within TTL, skip fetch.
     * 3. Fetch shared data via [syncService].
     * 4. On successful response, map records to [SyncControl.EntityShared].
     * 5. Clear existing records in the local database and insert the new batch.
     * 6. Update the last-download timestamp.
     *
     * @param previousDataTask  Result or output of the dependent task (unused).
     * @return [TaskResult.success] always, even on no-op or after completion.
     */
    override suspend fun execute(previousDataTask: Any?, weightProgressSum: Int, totalProgressWeight: Int): TaskResult {
        // Skip if offline
        if (networkValidator.isNetworkAvailable().not()) {
            return TaskResult.success()
        }

        val lastDateDownload = getLastDateDownload()

        // Skip if last download is within the TTL window
        if ((lastDateDownload?.diffInHoursFromNow() ?: Int.MAX_VALUE) < REFRESH_TTL) {
            return TaskResult.success()
        }

        // Fetch shared data from server
        val dataShared = syncService.getDataShared()

        if (dataShared is DataResult.Success) {
            val dataToInsert = mutableListOf<SyncControl.EntityShared>()

            dataShared.data.forEach { record ->
                val entityName = record.entity
                    ?: throw IllegalStateException("Entity name is null")
                val entityId = record.data?.get("id") as? String
                    ?: throw IllegalStateException("Entity ID is null")
                val entityData = record.data
                    ?: throw IllegalStateException("Entity data is null")

                dataToInsert.add(
                    SyncControl.EntityShared(
                        entityId,
                        entityName,
                        entityData
                    )
                )
            }

            // Refresh local storage
            databaseHelper.truncate()
            databaseHelper.insert(*dataToInsert.toTypedArray())
            updateLastDateDownload()
        }

        return TaskResult.success()
    }

    /**
     * Retrieves the timestamp of the last successful shared-data download.
     *
     * @return An [Instant] representing the last download time, or null if not set.
     */
    private fun getLastDateDownload(): Instant? =
        settings.getLongOrNull(KEY_LAST_DATE_DATA_SHARED)
            ?.let { Instant.fromEpochSeconds(it) }

    /**
     * Persists the current time as the last successful shared-data download timestamp.
     */
    private fun updateLastDateDownload() {
        settings.putLong(KEY_LAST_DATE_DATA_SHARED, Clock.System.now().epochSeconds)
    }

    companion object {
        /** Key under which the last download timestamp is stored in [Settings]. */
        const val KEY_LAST_DATE_DATA_SHARED = "last_date_data_shared"

        /** Time-to-live for shared-data refresh, in hours. */
        const val REFRESH_TTL = 24
    }
}
