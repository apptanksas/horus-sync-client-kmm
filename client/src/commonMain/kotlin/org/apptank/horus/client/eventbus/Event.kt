package org.apptank.horus.client.eventbus

import org.apptank.horus.client.base.DataMap

/**
 * Represents an event with optional data.
 *
 * @property data Optional data associated with the event.
 */
data class Event(val data: DataMap? = null)

/**
 * Enum class representing various types of events.
 */
enum class EventType {
    /**
     * Event type indicating that the client is ready to start synchronization.
     */
    ON_READY,

    /**
     * Event type indicating that pushing new data to the server was successful.
     */
    SYNC_PUSH_SUCCESS,

    /**
     * Event type indicating that pushing new data to the server failed.
     */
    SYNC_PUSH_FAILED,

    /**
     * Event type indicating that new actions were created and are pending synchronization with the server.
     */
    ACTION_CREATED,

    /**
     * Event type indicating that a new entity was created.
     */
    ENTITY_CREATED,

    /**
     * Event type indicating that an existing entity was updated.
     */
    ENTITY_UPDATED,

    /**
     * Event type indicating that an entity was deleted.
     */
    ENTITY_DELETED,

    /**
     * Event type indicating that the user setup has changed.
     */
    SETUP_CHANGED,

    /**
     * Event type indicating that a file has been queued for upload.
     */
    FILE_QUEUED_FOR_UPLOAD,

    /**
     * Event type indicating that user has logged out.
     */
    USER_SESSION_CLEARED,
}
