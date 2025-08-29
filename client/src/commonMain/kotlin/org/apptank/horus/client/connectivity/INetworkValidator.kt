package org.apptank.horus.client.connectivity

import org.apptank.horus.client.base.Callback

/**
 * Interface for validating network availability and handling network changes.
 *
 * This interface provides methods to check if the network is available and to register a callback
 * that will be invoked when network status changes.
 */
interface INetworkValidator {

    /**
     * Checks if the network is currently available.
     *
     * @return `true` if the network is available, `false` otherwise.
     */
    fun isNetworkAvailable(): Boolean

    /**
     * Registers a callback to be invoked when the network status changes.
     *
     * @param callback A function to be called when the network status changes.
     */
    fun onNetworkChange(callback: Callback)

    /**
     * Registers a network callback to monitor network changes.
     */
    fun registerNetworkCallback()

    /**
     * Gets the current connection level.
     *
     * @return The current [ConnectionLevel].
     */
    fun getNetworkInfo(): Network

    /**
     * Unregisters the network callback.
     */
    fun unregisterNetworkCallback()
}
