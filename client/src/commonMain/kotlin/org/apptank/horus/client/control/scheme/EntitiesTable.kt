package org.apptank.horus.client.control.scheme

internal object EntitiesTable {

    const val TABLE_NAME = "horus_entities"

    // Column names
    const val ATTR_NAME = "name" // Entity name
    const val ATTR_IS_WRITABLE = "is_writable" // Indicates if the entity is writable or only readable
    const val ATTR_LEVEL = "level" // Entity level, used for hierarchical structures

    // SQL statement to create the table if it does not exist
    const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ATTR_NAME TEXT PRIMARY KEY NOT NULL," +
                "$ATTR_IS_WRITABLE BOOLEAN NOT NULL," +
                "$ATTR_LEVEL INTEGER)"


    /**
     * Maps the details of an entity to a format suitable for insertion into the horus_entities table.
     *
     * @param entityName The name of the entity.
     * @param isWritable Indicates if the entity is writable.
     * @param level The level of the entity, used for hierarchical structures.
     */
    fun mapToCreate(entityName: String, isWritable: Boolean, level: Int) = mapOf(
        ATTR_NAME to entityName,
        ATTR_IS_WRITABLE to isWritable,
        ATTR_LEVEL to level
    )
}