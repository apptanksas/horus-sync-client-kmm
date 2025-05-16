package org.apptank.horus.client.tasks

import com.russhwolf.settings.Settings
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.IDataSharedDatabaseHelper
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.extensions.diffInHoursFromNow
import org.apptank.horus.client.sync.network.service.ISynchronizationService


internal class RefreshReadableEntitiesTask(
    private val settings: Settings,
    private val networkValidator: INetworkValidator,
    private val syncService: ISynchronizationService,
    dependsOnTask: RetrieveDataSharedTask
) : BaseTask(dependsOnTask) {

       override suspend fun execute(previousDataTask: Any?): TaskResult {
        // Skip if offline
        if (networkValidator.isNetworkAvailable().not()) {
            return TaskResult.success()
        }



           // syncService.getDataEntity()

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
