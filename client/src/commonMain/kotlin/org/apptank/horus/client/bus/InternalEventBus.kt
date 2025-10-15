package org.apptank.horus.client.bus

import org.apptank.horus.client.base.CallbackEvent


/**
 * Singleton object for managing event listeners and dispatching events.
 */
internal object InternalEventBus {

    /**
     * Map of event types to their associated listeners.
     */
    private val listeners = mutableMapOf<EventType, MutableList<CallbackEvent>>()

    /**
     * Registers a listener for a specific event type.
     *
     * @param eventType The type of event to listen for.
     * @param listener The callback function to be invoked when the event is emitted.
     */
    fun register(eventType: EventType, listener: CallbackEvent) {
        val eventListeners = listeners.getOrPut(eventType) { mutableListOf() }
        eventListeners.add(listener)
    }

    /**
     * Unregisters a listener for a specific event type.
     *
     * @param eventType The type of event to stop listening for.
     * @param listener The callback function to be removed from the event's listeners.
     */
    fun unregister(eventType: EventType, listener: CallbackEvent) {
        val eventListeners = listeners[eventType]
        eventListeners?.remove(listener)
    }

    /**
     * Emits an event of a specific type to all registered listeners.
     *
     * @param eventType The type of event to emit.
     * @param event The event data to pass to the listeners (default is an empty Event).
     */
    fun emit(eventType: EventType, event: Event = Event()) {
        listeners[eventType]?.toList()?.forEach { listener ->
            listener(event)
        }
    }

    /**
     * [TestOnly] Gets the number of listeners for a specific event type.
     *
     * @param eventType The type of event to count listeners for.
     * @return The number of listeners for the event type.
     */
    fun getCountListeners(eventType: EventType): Int {
        return listeners[eventType]?.size ?: 0
    }
}
