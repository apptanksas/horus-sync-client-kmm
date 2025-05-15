package org.apptank.horus.client.control.scheme

object DataSharedTable {

    const val TABLE_NAME = "horus_data_shared"

    // Column names
    const val ATTR_ID = "id"
    const val ATTR_ENTITY_NAME = "entity"
    const val ATTR_DATA = "data"

    // SQL statement to create the table if it does not exist
    const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ATTR_ID TEXT PRIMARY KEY NOT NULL," +
                "$ATTR_ENTITY_NAME TEXT NOT NULL," +
                "$ATTR_DATA TEXT NOT NULL)"

    fun mapToCreate(entityId: String, entityName: String, data: String) = mapOf(
        ATTR_ID to entityId,
        ATTR_ENTITY_NAME to entityName,
        ATTR_DATA to data
    )
}