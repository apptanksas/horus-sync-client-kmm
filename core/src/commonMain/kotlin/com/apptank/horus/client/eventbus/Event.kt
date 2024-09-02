package com.apptank.horus.client.eventbus

data class Event(val data: Map<String, Any>? = null)

enum class EventType {
    SYNC_READY,
    SYNC_PUSH_SUCCESS,
    SYNC_PUSH_FAILED,
    ACTION_CREATED,
    ENTITY_CREATED,
    ENTITY_UPDATED,
    ENTITY_DELETED,
}