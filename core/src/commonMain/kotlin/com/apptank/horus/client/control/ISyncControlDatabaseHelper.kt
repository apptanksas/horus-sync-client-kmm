package com.apptank.horus.client.control

import com.apptank.horus.client.data.Horus


interface ISyncControlDatabaseHelper {

    fun isStatusCompleted(type: SyncControl.OperationType): Boolean
    fun getLastDatetimeCheckpoint(): Long
    fun addSyncTypeStatus(type: SyncControl.OperationType, status: SyncControl.Status)
    fun addActionInsert(
        entity: String,
        attributes: List<Horus.Attribute<*>>
    )

    /**
     * Add an update action to the queue
     *
     * @param entity Entity name
     * @param id Entity ID
     * @param attributes List of attributes to update
     */
    fun addActionUpdate(
        entity: String,
        id: Horus.Attribute<String>,
        attributes: List<Horus.Attribute<*>>
    )

    /**
     * Add a delete action to the queue
     *
     * @param entity Entity name
     * @param id Entity ID
     */
    fun addActionDelete(
        entity: String,
        id: Horus.Attribute<String>
    )

    /**
     * Get pending actions from the queue
     *
     * @return List of pending actions
     */
    fun getPendingActions(): List<SyncControl.Action>

    /**
     * Update the status of the actions as completed
     */
    fun completeActions(actionIds: List<Int>): Boolean

    /**
     * Get the last action completed
     *
     * @return Last action completed
     */
    fun getLastActionCompleted(): SyncControl.Action?

    fun getCompletedActionsAfterDatetime(datetime: Long): List<SyncControl.Action>

    fun getEntityNames(): List<String>
}