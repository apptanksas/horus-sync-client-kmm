package org.apptank.horus.client.connectivity

data class Network(
    val connections: List<NetworkConnection>
) {
    fun hasValidConnection(): Boolean {
        return connections.any { it.level.level >= ConnectionLevel.MEDIUM.level }
    }

    fun hasLowConnection(): Boolean {
        return connections.any { it.level == ConnectionLevel.LOW || it.level == ConnectionLevel.VERY_LOW }
    }

    fun isOfflineMode(): Boolean {
        return hasValidConnection().not()
    }

    override fun toString(): String {
        return "Network(connections=$connections)"
    }

    companion object {
        fun noConnections() = Network(emptyList())
    }

}