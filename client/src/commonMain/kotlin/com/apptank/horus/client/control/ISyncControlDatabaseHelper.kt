package com.apptank.horus.client.control

import com.apptank.horus.client.data.Horus


/**
 * Interface defining methods for interacting with a database to manage synchronization control data.
 * This includes operations to check statuses, manage sync types, and handle actions such as inserts, updates, and deletions.
 *
 * @author John Ospina
 * @year 2024
 */
interface ISyncControlDatabaseHelper {

    /**
     * Checks if the status for a given operation type is completed.
     *
     * @param type The type of synchronization operation.
     * @return True if the status is completed, false otherwise.
     */
    fun isStatusCompleted(type: SyncControl.OperationType): Boolean

    /**
     * Retrieves the timestamp of the last datetime checkpoint.
     *
     * @return The timestamp of the last checkpoint in milliseconds.
     */
    fun getLastDatetimeCheckpoint(): Long

    /**
     * Adds a new synchronization type status to the database.
     *
     * @param type The type of synchronization operation.
     * @param status The status to be recorded.
     */
    fun addSyncTypeStatus(type: SyncControl.OperationType, status: SyncControl.Status)

    /**
     * Inserts a new action for an entity into the database.
     *
     * @param entity The name of the entity.
     * @param attributes The attributes of the entity to be inserted.
     */
    fun addActionInsert(
        entity: String,
        attributes: List<Horus.Attribute<*>>
    )

    /**
     * Updates an existing action for an entity in the database.
     *
     * @param entity The name of the entity.
     * @param id The identifier of the entity.
     * @param attributes The attributes to be updated.
     */
    fun addActionUpdate(
        entity: String,
        id: Horus.Attribute<String>,
        attributes: List<Horus.Attribute<*>>
    )

    /**
     * Deletes an action for an entity from the database.
     *
     * @param entity The name of the entity.
     * @param id The identifier of the entity.
     */
    fun addActionDelete(
        entity: String,
        id: Horus.Attribute<String>
    )

    /**
     * Retrieves a list of pending actions from the database.
     *
     * @return A list of pending synchronization actions.
     */
    fun getPendingActions(): List<SyncControl.Action>

    /**
     * Marks specified actions as completed.
     *
     * @param actionIds The identifiers of the actions to be marked as completed.
     * @return True if the actions were successfully marked as completed, false otherwise.
     */
    fun completeActions(actionIds: List<Int>): Boolean

    /**
     * Retrieves the last completed action from the database.
     *
     * @return The last completed synchronization action, or null if no actions have been completed.
     */
    fun getLastActionCompleted(): SyncControl.Action?

    /**
     * Retrieves all completed actions that occurred after a specified datetime.
     *
     * @param datetime The timestamp to filter completed actions.
     * @return A list of completed synchronization actions that occurred after the specified datetime.
     */
    fun getCompletedActionsAfterDatetime(datetime: Long): List<SyncControl.Action>

    /**
     * Retrieves a list of all entity names from the database.
     *
     * @return A list of entity names.
     */
    fun getEntityNames(): List<String>

    /**
     * Retrieves a list of all entity names that can be written to.
     *
     * @return A list of entity names.
     */
    fun getWritableEntityNames(): List<String>

    /**
     * Checks if an entity can be written to.
     *
     * @param entityName The name of the entity to check.
     * @return True if the entity can be written to, false otherwise.
     */
    fun isEntityCanBeWritable(entityName: String): Boolean
}
