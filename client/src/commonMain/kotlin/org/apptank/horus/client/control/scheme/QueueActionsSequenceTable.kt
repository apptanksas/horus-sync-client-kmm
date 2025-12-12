package org.apptank.horus.client.control.scheme

/**
 * Defines the schema and utility functions for the `sync_control_sequence` table.
 */
object QueueActionsSequenceTable {

    const val TABLE_NAME = "horus_queue_actions_sequence"

    // Column names
    const val ATTR_SEQUENCE = "sequence"

    // SQL statement to create the table if it does not exist
    const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ATTR_SEQUENCE STRING PRIMARY KEY NOT NULL" +
                ")"

    /**
     * Maps an `OperationType` and `Status` to a `Map` for insertion into the `sync_control` table.
     *
     * @param type The type of synchronization operation.
     * @param status The status of the synchronization.
     * @return A map of column names to values for insertion.
     */
    fun mapToCreate(sequence: Long) = mapOf(
        ATTR_SEQUENCE to sequence.toString()
    )
}