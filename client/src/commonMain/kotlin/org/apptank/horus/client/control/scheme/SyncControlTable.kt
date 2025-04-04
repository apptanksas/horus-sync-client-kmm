package org.apptank.horus.client.control.scheme

import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.utils.SystemTime

/**
 * Object that defines the schema and utility functions for the `sync_control` table in the database.
 */
internal object SyncControlTable {

    const val TABLE_NAME = "horus_sync_control"

    // Column names
    const val ATTR_ID = "id"
    const val ATTR_TYPE = "type"
    const val ATTR_STATUS = "status"
    const val ATTR_DATETIME = "datetime"

    // SQL statement to create the table if it does not exist
    const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ATTR_ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "$ATTR_TYPE INTEGER NOT NULL," +
                "$ATTR_STATUS INTEGER NOT NULL," +
                "$ATTR_DATETIME INTEGER NOT NULL)"

    /**
     * Maps an `OperationType` and `Status` to a `Map` for insertion into the `sync_control` table.
     *
     * @param type The type of synchronization operation.
     * @param status The status of the synchronization.
     * @return A map of column names to values for insertion.
     */
    fun mapToCreate(type: SyncControl.OperationType, status: SyncControl.Status) = mapOf(
        ATTR_TYPE to type.id,
        ATTR_STATUS to status.id,
        ATTR_DATETIME to SystemTime.getCurrentTimestamp()
    )
}
