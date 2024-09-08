package com.apptank.horus.client.eventbus

import com.apptank.horus.client.base.DataMap

data class Event(val data: DataMap? = null)

enum class EventType {
    VALIDATION_COMPLETED, // Sync validation completed
    SYNC_PUSH_SUCCESS, // Push new data to server is successful
    SYNC_PUSH_FAILED, // Push new data to server failed
    ACTION_CREATED, // New actions created and pending to be sync with the server
    ENTITY_CREATED, // New entity was created
    ENTITY_UPDATED, // Entity was updated
    ENTITY_DELETED, // Entity was deleted
}