package com.apptank.horus.client.control

internal object EntitiesTable {

    const val TABLE_NAME = "horus_entities"

    // Column names
    const val ATTR_NAME = "name"
    const val ATTR_IS_WRITABLE = "is_writable"

    // SQL statement to create the table if it does not exist
    const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ATTR_NAME TEXT PRIMARY KEY NOT NULL," +
                "$ATTR_IS_WRITABLE BOOLEAN NOT NULL)"


    /**
     * Maps the details of an entity to a format suitable for insertion into the horus_entities table.
     *
     * @param entityName The name of the entity.
     * @param isWritable Indicates if the entity is writable.
     */
    fun mapToCreate(entityName: String, isWritable: Boolean) = mapOf(
        ATTR_NAME to entityName,
        ATTR_IS_WRITABLE to isWritable
    )
}