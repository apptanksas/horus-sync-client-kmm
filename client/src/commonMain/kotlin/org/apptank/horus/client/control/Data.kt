package org.apptank.horus.client.control

import org.apptank.horus.client.base.DataMap
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * SyncControl is a sealed class that encapsulates various operations, statuses, and actions used for synchronization control.
 * It provides a structure for handling synchronization processes such as validations, initial syncs, and checkpoints.
 *
 * The class contains enums for different operation types, statuses, actions, and their status, along with
 * a data class representing an action with its details and utility functions for timestamps and entity data retrieval.
 *
 * @sealed
 * @author John Ospina
 * @year 2024
 */
sealed class SyncControl {

    /**
     * Enum representing the type of synchronization operation.
     *
     * @param id The identifier for the operation type.
     */
    enum class OperationType(val id: Int) {
        HASH_VALIDATION(1),
        INITIAL_SYNCHRONIZATION(2),
        CHECKPOINT(3),
    }

    /**
     * Enum representing the status of a synchronization operation.
     *
     * @param id The identifier for the status.
     */
    enum class Status(val id: Int) {
        COMPLETED(1),
        FAILED(2);

        companion object {
            /**
             * Retrieves the Status enum based on its ID.
             *
             * @param id The identifier for the status.
             * @return The corresponding Status enum.
             * @throws IllegalArgumentException If the ID does not correspond to any valid status.
             */
            fun fromId(id: Int): Status =
                entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
        }
    }

    /**
     * Enum representing the type of action being performed during synchronization.
     *
     * @param id The identifier for the action type.
     */
    enum class ActionType(val id: Int) {
        INSERT(1),
        UPDATE(2),
        DELETE(3);

        companion object {
            /**
             * Retrieves the ActionType enum based on its ID.
             *
             * @param id The identifier for the action type.
             * @return The corresponding ActionType enum.
             * @throws IllegalArgumentException If the ID does not correspond to any valid action type.
             */
            fun fromId(id: Int): ActionType =
                entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
        }
    }

    /**
     * Enum representing the status of an action performed during synchronization.
     *
     * @param id The identifier for the action status.
     */
    enum class ActionStatus(val id: Int) {
        PENDING(1),
        COMPLETED(2);

        companion object {
            /**
             * Retrieves the ActionStatus enum based on its ID.
             *
             * @param id The identifier for the action status.
             * @return The corresponding ActionStatus enum.
             * @throws IllegalArgumentException If the ID does not correspond to any valid action status.
             */
            fun fromId(id: Int): ActionStatus =
                entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
        }
    }

    /**
     * Enum representing the status of a file.
     *
     * @param id The identifier for the file status.
     */
    enum class FileStatus(val id: Int) {
        LOCAL(1),
        REMOTE(2),
        SYNCED(3);

        companion object {
            /**
             * Retrieves the FileStatus enum based on its ID.
             *
             * @param id The identifier for the file status.
             * @return The corresponding FileStatus enum.
             * @throws IllegalArgumentException If the ID does not correspond to any valid file status.
             */
            fun fromId(id: Int): FileStatus =
                entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
        }
    }

    /**
     * Enum representing the type of file.
     *
     * @param id The identifier for the file type.
     */
    enum class FileType(val id: Int) {
        FILE(0),
        IMAGE(1);

        companion object {
            /**
             * Retrieves the FileType enum based on its ID.
             *
             * @param id The identifier for the file type.
             * @return The corresponding FileType enum.
             * @throws IllegalArgumentException If the ID does not correspond to any valid file type.
             */
            fun fromId(id: Int): FileType =
                entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
        }
    }

    /**
     * Data class representing a file to be synchronized.
     *
     * @param reference The unique reference for the file.
     * @param type The type of file.
     * @param status The status of the file.
     * @param urlLocal The local URL of the file.
     * @param urlRemote The remote URL of the file.
     * @param mimeType The MIME type of the file.
     */
    data class File(
        val reference: CharSequence,
        val type: FileType,
        val status: FileStatus,
        val mimeType: String,
        val urlLocal: String? = null,
        val urlRemote: String? = null
    )


    /**
     * Data class representing an action performed during synchronization.
     *
     * @param id The unique identifier for the action.
     * @param action The type of action being performed.
     * @param entity The name of the entity being affected.
     * @param status The status of the action.
     * @param data A map of data related to the action.
     * @param actionedAt The time at which the action was performed.
     */
    data class Action(
        val id: Int,
        val action: ActionType,
        val entity: String,
        val status: ActionStatus,
        val data: DataMap,
        val actionedAt: LocalDateTime
    ) {

        /**
         * Retrieves the timestamp for when the action was performed in epoch seconds.
         *
         * @return The timestamp of the action in epoch seconds.
         */
        fun getActionedAtTimestamp(): Long {
            return actionedAt.toInstant(
                TimeZone.UTC
            ).epochSeconds
        }

        /**
         * Retrieves the ID of the entity associated with the action.
         *
         * @return The entity ID as a string.
         */
        fun getEntityId(): String {
            return data["id"] as String
        }

        /**
         * Retrieves the attributes of the entity associated with the action.
         *
         * @return A map containing the entity attributes.
         */
        fun getEntityAttributes(): DataMap {
            return data["attributes"] as DataMap
        }
    }

    /**
     * Data class representing a shared entity.
     *
     * @param entityId The unique identifier for the entity.
     * @param entityName The name of the entity.
     * @param data A map of data related to the entity.
     */
    data class EntityShared(
        val entityId: String,
        val entityName: String,
        val data: DataMap
    )
}


