package org.apptank.horus.client.control.model

/**
 * Data class representing a relationship between two entities.
 * @param entity The name of the related entity.
 * @param attributesLinked A list of attributes that are linked to the entity.
 */
data class EntityRelated(
    val entity: String,
    val attributesLinked: List<String>
)
