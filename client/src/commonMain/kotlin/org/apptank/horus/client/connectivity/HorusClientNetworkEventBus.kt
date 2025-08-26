package org.apptank.horus.client.connectivity

object HorusClientNetworkEventBus {

    private val listeners = mutableListOf<(Network) -> Unit>()

    fun register(listener: (Network) -> Unit) {
        listeners.add(listener)
    }

    fun unregister(listener: (Network) -> Unit) {
        listeners.remove(listener)
    }

    fun emit(network: Network) {
        listeners.toList().forEach { listener ->
            listener(network)
        }
    }
}