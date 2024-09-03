package com.apptank.horus.client.control

import com.apptank.horus.client.utils.SystemTime

object SyncControlTable {

    const val TABLE_NAME = "sync_control"

    const val ATTR_ID = "id"
    const val ATTR_TYPE = "type"
    const val ATTR_STATUS = "status"
    const val ATTR_DATETIME = "datetime"

    const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ATTR_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$ATTR_TYPE INTEGER," +
                "$ATTR_STATUS INTEGER," +
                "$ATTR_DATETIME INTEGER)"

    fun mapToCreate(type: SyncControl.OperationType, status: SyncControl.Status) = mapOf(
        ATTR_TYPE to type.id,
        ATTR_STATUS to status.id,
        ATTR_DATETIME to SystemTime.getCurrentTimestamp()
    )
}