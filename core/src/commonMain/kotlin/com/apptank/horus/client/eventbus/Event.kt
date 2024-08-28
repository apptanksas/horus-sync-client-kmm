package com.apptank.horus.client.eventbus

data class Event(val data: MutableMap<String, Any>? = null)

enum class EventType {
    SYNC_READY,
    ACTION_CREATED,
    ENTITY_CREATED,
    ENTITY_UPDATED,
    ENTITY_DELETED,
}