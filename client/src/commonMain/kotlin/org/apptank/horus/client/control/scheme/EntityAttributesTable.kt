package org.apptank.horus.client.control.scheme

import org.apptank.horus.client.migration.domain.AttributeType

/**
 * EntityAttributesTable is a singleton object that stores the table name and column names for the entity attributes table.
 *
 * @author John Ospina
 * @year 2024
 */
object EntityAttributesTable {

    const val TABLE_NAME = "horus_entity_attributes"

    // Column names
    const val ATTR_ID = "id"
    const val ATTR_ENTITY_NAME = "entity_name"
    const val ATTR_ATTRIBUTE_NAME = "attribute_name"
    const val ATTR_TYPE = "type"

    // SQL statement to create the table if it does not exist
    const val SQL_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ATTR_ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "$ATTR_ENTITY_NAME TEXT NOT NULL," +
                "$ATTR_ATTRIBUTE_NAME TEXT NOT NULL," +
                "$ATTR_TYPE TEXT NOT NULL)"

    /**
     * Returns a map with the values to insert a new row in the table.
     *
     * @param entityName The name of the entity.
     * @param attributeName The name of the attribute.
     * @param attributeType The type of the attribute.
     * @return The map with the values.
     */
    fun mapToCreate(entityName: String, attributeName: String, attributeType: AttributeType) =
        mapOf(
            ATTR_ENTITY_NAME to entityName,
            ATTR_ATTRIBUTE_NAME to attributeName,
            ATTR_TYPE to attributeType.name
        )
}