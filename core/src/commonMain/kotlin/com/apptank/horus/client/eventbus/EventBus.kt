package com.apptank.horus.client.eventbus


typealias CallbackEvent = (Event) -> Unit
/**
 * This class is a simple event bus implementation.
 */
object EventBus {

    /**
     * A map of event types to their listeners.
     */
    private val listeners = mutableMapOf<EventType, MutableList<CallbackEvent>>()

    /**
     * Register a listener for a specific event type.
     */
    fun register(eventType: EventType, listener: CallbackEvent) {
        val eventListeners = listeners.getOrPut(eventType) { mutableListOf() }
        eventListeners.add(listener)
    }

    /**
     * Unregister a listener for a specific event type.
     */
    fun unregister(eventType: EventType, listener: CallbackEvent) {
        val eventListeners = listeners[eventType]
        eventListeners?.remove(listener as (Event) -> Unit)
    }

    /**
     * Post an event to all listeners of the event type.
     */
    fun post(eventType: EventType, event: Event = Event()) {
        listeners[eventType]?.forEach { listener ->
            listener(event)
        }
    }
}