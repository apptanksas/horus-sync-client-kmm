package org.apptank.horus.client.connectivity

data class Network(
    val connections: List<NetworkConnection>
) {
    fun hasValidConnection(): Boolean {
        return connections.any { it.level.level >= ConnectionLevel.MEDIUM.level }
    }

    companion object {
        fun noConnections() = Network(emptyList())
    }


}