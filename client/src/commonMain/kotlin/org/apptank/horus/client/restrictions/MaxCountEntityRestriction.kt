package org.apptank.horus.client.restrictions

/**
 * MaxCountEntityRestriction is a data class that represents a restriction on the maximum number of rows that can be stored in a entity.
 *
 * @param entity The name of the entity.
 * @param maxCount The maximum number of rows that can be stored in the entity.
 */
data class MaxCountEntityRestriction(
    val entity: String,
    val maxCount: Int
) : EntityRestriction {

    /**
     * Gets the name of the entity.
     *
     * @return The name of the entity.
     */
    override fun getEntityName(): String {
        return entity
    }
}