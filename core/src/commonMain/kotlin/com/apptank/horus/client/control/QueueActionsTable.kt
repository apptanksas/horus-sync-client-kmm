package com.apptank.horus.client.control

import com.apptank.horus.client.serialization.AnySerializer
import com.apptank.horus.client.utils.SystemTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object QueueActionsTable {

    const val TABLE_NAME = "queue_actions"

    const val ATTR_ID = "id"
    const val ATTR_ACTION_TYPE = "action_type"
    const val ATTR_ENTITY = "entity"
    const val ATTR_DATA = "data"
    const val ATTR_STATUS = "status"
    const val ATTR_DATETIME = "datetime"

    const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ATTR_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$ATTR_ACTION_TYPE INTEGER," +
                "$ATTR_ENTITY TEXT," +
                "$ATTR_DATA TEXT," +
                "$ATTR_STATUS INTEGER," +
                "$ATTR_DATETIME INTEGER)"

    fun mapToCreate(actionType: SyncActionType, entity: String, jsonData: Map<String, @Serializable(with = AnySerializer::class)  Any?>) = mapOf(
        ATTR_ACTION_TYPE to actionType.id,
        ATTR_ENTITY to entity,
        ATTR_DATA to Json.encodeToString(jsonData),
        ATTR_STATUS to SyncActionStatus.PENDING.id,
        ATTR_DATETIME to SystemTime.getCurrentTimestamp()
    )
}