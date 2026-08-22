package org.apptank.horus.client.config

/**
 * The `HorusPreferences` object stores global preference settings for the Horus synchronization system.
 * These settings can be modified at runtime to adjust the client's behavior.
 *
 * @property ignoreNetworkStatus A flag indicating whether to ignore the network connectivity status during synchronization.
 *                               When set to `true`, the system will attempt to synchronize even if no network connection is detected.
 * @property offlineMode A flag indicating whether the system should operate in offline mode.
 *                       When set to `true`, no network requests will be made, and the system will rely on local data.
 */
object HorusPreferences {
    var ignoreNetworkStatus: Boolean = false
    var offlineMode: Boolean = false
}