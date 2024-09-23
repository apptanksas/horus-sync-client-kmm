package org.apptank.horus.client.data

import org.apptank.horus.client.base.DataMap

/**
 * Interface for listening to data change events, including insertions, updates, and deletions.
 */
interface DataChangeListener {
    /**
     * Called when a new entity is inserted.
     *
     * @param entity The name of the entity that was inserted.
     * @param id The identifier of the inserted entity.
     * @param data A map containing the attributes of the inserted entity.
     */
    fun onInsert(entity: String, id: String, data: DataMap)

    /**
     * Called when an existing entity is updated.
     *
     * @param entity The name of the entity that was updated.
     * @param id The identifier of the updated entity.
     * @param data A map containing the updated attributes of the entity.
     */
    fun onUpdate(entity: String, id: String, data: DataMap)

    /**
     * Called when an entity is deleted.
     *
     * @param entity The name of the entity that was deleted.
     * @param id The identifier of the deleted entity.
     */
    fun onDelete(entity: String, id: String)
}
