package org.apptank.horus.client.connectivity

import org.apptank.horus.client.config.HorusPreferences

data class Network(
    val connections: List<NetworkConnection>
) {
    fun hasValidConnection(): Boolean {

        if (HorusPreferences.offlineMode) {
            return false
        }

        if (HorusPreferences.ignoreNetworkStatus) {
            return true
        }

        return connections.any { it.level.level >= ConnectionLevel.MEDIUM.level }
    }

    fun hasLowConnection(): Boolean {
        return connections.any { it.level == ConnectionLevel.LOW || it.level == ConnectionLevel.VERY_LOW }
    }

    fun isOfflineMode(): Boolean {

        if (HorusPreferences.offlineMode) {
            return true
        }

        if (HorusPreferences.ignoreNetworkStatus) {
            return false
        }

        return hasValidConnection().not()
    }

    override fun toString(): String {
        return "Network(connections=$connections)"
    }

    companion object {
        fun noConnections() = Network(emptyList())
    }

}