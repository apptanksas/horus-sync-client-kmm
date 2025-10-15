package org.apptank.horus.client.bus

import org.apptank.horus.client.connectivity.Network

object HorusClientNetworkEventBus {

    private val listeners = mutableListOf<(Network) -> Unit>()
    private var lastNetworkState: Network? = null

    fun register(listener: (Network) -> Unit) {
        listeners.add(listener)
        lastNetworkState?.let { emitNetworkChange(it) }
    }

    fun unregister(listener: (Network) -> Unit) {
        listeners.remove(listener)
    }

    fun emit(network: Network) {
        lastNetworkState = network
        emitNetworkChange(network)
    }


    private fun emitNetworkChange(network: Network) {
        listeners.toList().forEach { listener ->
            listener(network)
        }
    }
}