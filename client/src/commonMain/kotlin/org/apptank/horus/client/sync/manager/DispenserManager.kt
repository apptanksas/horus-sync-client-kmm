package org.apptank.horus.client.sync.manager

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.apptank.horus.client.control.ISyncControlDatabaseHelper
import org.apptank.horus.client.extensions.info

private const val TAG = "DispenserManager"
/**
 * The `DispenserManager` class is responsible for managing the synchronization of pending actions
 * with a remote server in batches. It tracks the number of actions and ensures that synchronization
 * occurs either when a batch reaches a defined size or when a specified expiration time has passed
 * since the last completed action.
 *
 * @property batchSize The number of actions required to trigger a synchronization.
 * @property expirationTime The maximum time allowed between synchronizations before forcing a new one.
 * @property syncControlDatabaseHelper Provides access to the database to retrieve pending actions and the last completed action.
 * @property remoteSynchronizatorManager Handles the remote synchronization process.
 */
internal class DispenserManager(
    private val batchSize: Int,
    private val expirationTime: Long,
    private val syncControlDatabaseHelper: ISyncControlDatabaseHelper,
    private val remoteSynchronizatorManager: RemoteSynchronizatorManager
) {
    // Tracks the current count of processed actions before synchronization is triggered
    private var batchCounter = 0

    /**
     * The `processBatch` method is responsible for handling the batching logic and
     * triggering synchronization. It increases the batch counter on each call and checks
     * whether the synchronization should be triggered based on batch size or expiration time.
     *
     * It will attempt to synchronize with the remote server if either condition is met:
     * - The number of actions in the current batch exceeds or equals the configured `batchSize`.
     * - The time since the last completed action exceeds the configured `expirationTime`.
     *
     * Upon successful synchronization, the batch counter is reset.
     */
    fun processBatch() {
        batchCounter++

        // Current timestamp in seconds
        val currentTimestamp = Clock.System.now().epochSeconds

        // Retrieve the timestamp of the last completed action, or 0 if none exists
        val lastActionCompletedTimestamp: Long =
            syncControlDatabaseHelper.getLastActionCompleted()?.actionedAt?.toInstant(TimeZone.UTC)?.epochSeconds
                ?: 0

        // Check if batch size threshold has been met
        val mustSynchronizeByBatch =
            batchCounter >= batchSize && syncControlDatabaseHelper.getPendingActions().size >= batchSize

        // Check if the time since the last completed action exceeds the expiration time
        val mustSynchronizeByTime =
            currentTimestamp - lastActionCompletedTimestamp >= expirationTime && lastActionCompletedTimestamp > 0L

        // Log information and trigger synchronization if needed
        if (mustSynchronizeByBatch) {
            info("[$TAG] Batch size reached")
        }

        if (mustSynchronizeByTime) {
            info("[$TAG] Expiration time reached")
        }

        if (mustSynchronizeByBatch || mustSynchronizeByTime) {
            info("Pushing pending actions to server...")
            remoteSynchronizatorManager.trySynchronizeData()
            batchCounter = 0
        }
    }
}
