package com.apptank.horus.client.control

import com.apptank.horus.client.serialization.AnySerializer
import com.apptank.horus.client.utils.SystemTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Object containing constants and utility functions related to the "queue_actions" table in the database.
 * This table stores actions that need to be processed, including details such as action type, entity, data, status, and timestamp.
 *
 * @author John Ospina
 * @year 2024
 */
internal object QueueActionsTable {

    // Name of the table in the database
    const val TABLE_NAME = "horus_queue_actions"

    // Column names in the table
    const val ATTR_ID = "id"
    const val ATTR_ACTION_TYPE = "action_type"
    const val ATTR_ENTITY = "entity"
    const val ATTR_DATA = "data"
    const val ATTR_STATUS = "status"
    const val ATTR_DATETIME = "datetime"

    /**
     * SQL statement to create the queue_actions table if it does not already exist.
     */
    const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ATTR_ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "$ATTR_ACTION_TYPE INTEGER NOT NULL," +
                "$ATTR_ENTITY TEXT NOT NULL," +
                "$ATTR_DATA TEXT NOT NULL," +
                "$ATTR_STATUS INTEGER NOT NULL," +
                "$ATTR_DATETIME INTEGER NOT NULL)"

    /**
     * Maps the details of an action to a format suitable for insertion into the queue_actions table.
     *
     * @param actionType The type of action to be performed.
     * @param entity The name of the entity associated with the action.
     * @param jsonData The data associated with the action, serialized as a JSON string.
     * @return A map of column names to values for insertion into the table.
     */
    fun mapToCreate(actionType: SyncControl.ActionType, entity: String, jsonData: Map<String, @Serializable(with = AnySerializer::class) Any?>) = mapOf(
        ATTR_ACTION_TYPE to actionType.id,
        ATTR_ENTITY to entity,
        ATTR_DATA to Json.encodeToString(jsonData),
        ATTR_STATUS to SyncControl.ActionStatus.PENDING.id,
        ATTR_DATETIME to SystemTime.getCurrentTimestamp()
    )
}
