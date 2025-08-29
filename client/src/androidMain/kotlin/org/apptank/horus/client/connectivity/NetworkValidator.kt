package org.apptank.horus.client.connectivity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.logException
import org.apptank.horus.client.extensions.warn

/**
 * NetworkValidator
 *
 * Provides a single place to observe and query the device's network state.
 * This implementation combines:
 *  - Connectivity information via ConnectivityManager (transport type: WIFI, CELLULAR, ETHERNET)
 *  - Signal strength and telephony display info via SignalStrengthMonitor (TelephonyCallback / PhoneStateListener)
 *
 * Responsibilities:
 *  - Expose synchronous queries about current network state via getNetworkInfo().
 *  - Register/unregister system network callbacks to receive connectivity changes.
 *  - Emit network-change events to an internal callback and to the HorusClientNetworkEventBus.
 *
 * Behavior and design notes:
 *  - Uses ConnectivityManager.registerDefaultNetworkCallback(...) to detect availability and capability changes.
 *  - Uses SignalStrengthMonitor to observe telephony signal-related events and triggers a network event when telephony signals change.
 *  - When permissions required to access telephony details are missing, the validator logs a warning and returns a conservative
 *    ConnectionLevel (MEDIUM) for mobile networks; this prevents SecurityException and provides a reasonable fallback.
 *  - Emitting network change triggers both the local `networkChangeCallback` and sends the updated `Network` object to
 *    HorusClientNetworkEventBus.
 *
 * Permissions required (declare in AndroidManifest and request at runtime):
 *  - android.permission.ACCESS_NETWORK_STATE (read-only; generally not runtime)
 *  - android.permission.ACCESS_WIFI_STATE
 *  - android.permission.ACCESS_FINE_LOCATION (required for getAllCellInfo() / accurate cell data)
 *  - android.permission.READ_PHONE_STATE (may be required on some devices / OS versions for telephony APIs)
 *
 * Important runtime considerations:
 *  - The telephony/cell APIs are restricted on modern Android versions; even with permissions granted, some vendors or OS
 *    releases may limit data. Test on real devices to validate behavior (emulator telephony simulation is limited).
 *  - Register/unregister network callbacks and the SignalStrengthMonitor according to your component lifecycle (e.g., start
 *    in Activity.onStart / Fragment.onStart; stop in onStop) to avoid leaks.
 *
 * External dependencies & expected project types:
 *  - INetworkValidator: the interface implemented by this class.
 *  - Network, NetworkConnection, ConnectionType, ConnectionLevel: domain types used to model the network state.
 *  - SignalStrengthMonitor: helper that monitors telephony signal/display changes (must be implemented separately).
 *  - HorusClientNetworkEventBus: event bus used for global network events; replace with your own event mechanism if different.
 *
 * Threading:
 *  - ConnectivityManager.NetworkCallback methods are invoked on system threads; the class responds by calling emitNetworkChange(),
 *    which is synchronous. If emitNetworkChange() triggers UI work, dispatch to the main thread.
 *  - SignalStrengthMonitor should deliver telephony callbacks on an executor you control (SignalStrengthMonitor currently uses
 *    context.mainExecutor in its implementation), which simplifies UI updates.
 *
 * Example usage:
 * ```
 * val validator = NetworkValidator(context)
 * validator.onNetworkChange {
 *   // react to changes (small, fast work)
 *   val network = validator.getNetworkInfo()
 *   // update UI or forward to viewmodel
 * }
 * validator.registerNetworkCallback()
 *
 * // later (e.g., onStop/destroy):
 * validator.unregisterNetworkCallback()
 * ```
 *
 * Recommendations:
 *  - Request required runtime permissions in the UI layer (Activity/Fragment) before calling registerNetworkCallback().
 *  - Prefer lifecycle-bound registration/unregistration to avoid leaks.
 *  - Test mobile-signal behavior on a physical device with a SIM card. Emulators do not reliably emulate signal strength or cell info.
 */
internal class NetworkValidator(
    private val context: Context
) : INetworkValidator {

    // Lazily obtain ConnectivityManager; may be null on extremely constrained contexts.
    private val manager: ConnectivityManager? by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    // Delegates telephony/signal listening to a dedicated monitor.
    private val signalStrengthMonitor by lazy {
        SignalStrengthMonitor(context)
    }

    // Optional local callback invoked when any network-related event happens.
    private var networkChangeCallback: (() -> Unit)? = null

    /**
     * A network callback used to observe broad connectivity changes reported by ConnectivityManager.
     * It reacts to availability, capability changes, losing/unavailable events and emits a consolidated network change.
     *
     * Note: ConnectivityManager callbacks do not provide per-cell signal strength — that is observed by SignalStrengthMonitor.
     */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: android.net.Network) {
            super.onAvailable(network)
            emitNetworkChange()
        }

        override fun onLost(network: android.net.Network) {
            super.onLost(network)
            emitNetworkChange()
        }

        override fun onCapabilitiesChanged(network: android.net.Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            emitNetworkChange()
        }

        override fun onUnavailable() {
            super.onUnavailable()
            emitNetworkChange()
        }

        override fun onLosing(network: android.net.Network, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            emitNetworkChange()
        }
    }

    /**
     * Checks synchronously if the network is available and the computed Network object reports a valid connection.
     *
     * @return true if the system reports a network is available and the domain-level Network is valid.
     * @throws SecurityException if called while lacking required permissions for underlying calls (suppressed via annotations).
     */
    @SuppressLint("MissingPermission")
    override fun isNetworkAvailable(): Boolean {
        return isNetworkIsAvailableByService() && getNetworkInfo().hasValidConnection()
    }

    /**
     * Register a callback to be invoked on network change events.
     * Also attaches a listener to the SignalStrengthMonitor so telephony-related changes will trigger emitNetworkChange().
     *
     * @param callback function invoked when network changes. May be called from different threads depending on monitors used.
     */
    override fun onNetworkChange(callback: () -> Unit) {
        networkChangeCallback = callback
        signalStrengthMonitor.setOnSignalChangedListener { emitNetworkChange() }
        // Emit the current information immediately via the shared event bus.
        HorusClientNetworkEventBus.emit(getNetworkInformation(context))
    }

    /**
     * Register the ConnectivityManager network callback and start the SignalStrengthMonitor.
     *
     * Important: ensure runtime permissions (READ_PHONE_STATE, ACCESS_FINE_LOCATION) have been requested by the UI layer
     * before calling this method if you expect mobile signal details to be available.
     */
    @SuppressLint("MissingPermission")
    override fun registerNetworkCallback() {
        manager?.registerDefaultNetworkCallback(networkCallback)
        signalStrengthMonitor.startListening()
    }

    /**
     * Returns a domain-level representation of the current network state.
     * This method queries ConnectivityManager and — if available — augments the result with Wi-Fi RSSI or cellular signal level.
     *
     * @return Network object containing zero or more NetworkConnection entries.
     */
    override fun getNetworkInfo(): Network {
        return getNetworkInformation(context)
    }

    /**
     * Unregisters the ConnectivityManager callback and stops the SignalStrengthMonitor.
     * This call should be invoked during lifecycle teardown to avoid leaks.
     */
    override fun unregisterNetworkCallback() {
        manager?.unregisterNetworkCallback(networkCallback)
        signalStrengthMonitor.stopListening()
    }

    /**
     * Emits the configured local callback and posts the latest Network state to HorusClientNetworkEventBus.
     * Side-effect: logs the computed Network information for debugging.
     */
    private fun emitNetworkChange() {
        networkChangeCallback?.invoke()
        with(getNetworkInformation(context)) {
            HorusClientNetworkEventBus.emit(this)
            info("[NetworkValidator] Network changed: $this")
        }
    }

    /**
     * Build a Network value from the current ConnectivityManager state and signal levels.
     * - Adds a WIFI connection entry when transport WIFI is present (level computed from WifiManager.rssi).
     * - Adds a CELLULAR connection entry when transport CELLULAR is present (level computed from TelephonyManager/allCellInfo).
     *
     * Returns Network.noConnections() when no active network or capabilities are present.
     */
    @SuppressLint("MissingPermission")
    private fun getNetworkInformation(context: Context): Network {

        if (!hasAccessNetworkStatePermission()) {
            logException("[NetworkValidator] Missing ACCESS_NETWORK_STATE permission; cannot access network state.")
            return Network.noConnections()
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return Network.noConnections()
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return Network.noConnections()
        val connections = mutableListOf<NetworkConnection>()

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            connections.add(NetworkConnection(type = ConnectionType.WIFI, level = getWifiSignalLevel(context)))
        }

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            connections.add(NetworkConnection(type = ConnectionType.CELLULAR, level = getMobileNetworkType(context)))
        }

        return Network(connections)
    }

    /**
     * Compute Wi-Fi signal level (0..4 converted to ConnectionLevel) using WifiManager.rssi.
     */
    private fun getWifiSignalLevel(context: Context): ConnectionLevel {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        val level = WifiManager.calculateSignalLevel(info.rssi, 5)

        return mapSignalLevel(level)
    }

    /**
     * Determines the ConnectionLevel for the current mobile network.
     *
     * Behavior notes:
     *  - If required telephony permissions are missing, the method logs a warning and returns ConnectionLevel.MEDIUM as a safe fallback.
     *  - Uses TelephonyManager.networkType to classify the generation (2G/3G/4G/5G).
     *  - For 3G/4G/5G entries, returns a mapped signal level based on getMobileSignalLevel().
     */
    @SuppressLint("MissingPermission")
    private fun getMobileNetworkType(context: Context): ConnectionLevel {

        if (hasPermissions(context).not()) {
            warn("[NetworkValidator] Missing permissions to access telephony state")
            return ConnectionLevel.MEDIUM
        }

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        return when (telephonyManager.networkType) {
            // 2G
            TelephonyManager.NETWORK_TYPE_GSM,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> ConnectionLevel.NONE

            // 3G
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> mapSignalLevel(getMobileSignalLevel(context))

            // 4G
            TelephonyManager.NETWORK_TYPE_LTE,
            TelephonyManager.NETWORK_TYPE_IWLAN -> mapSignalLevel(getMobileSignalLevel(context))

            // 5G
            TelephonyManager.NETWORK_TYPE_NR -> mapSignalLevel(getMobileSignalLevel(context))

            TelephonyManager.NETWORK_TYPE_UNKNOWN -> ConnectionLevel.NONE
            else -> ConnectionLevel.NONE
        }
    }

    /**
     * Returns the mobile signal strength level (0-4) for the primary cell (if available).
     * Requires ACCESS_FINE_LOCATION (and sometimes READ_PHONE_STATE) to return real cell data.
     * Returns -1 when no cell info is available.
     */
    @SuppressLint("MissingPermission")
    private fun getMobileSignalLevel(context: Context): Int {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cellInfoList = telephonyManager.allCellInfo
        if (!cellInfoList.isNullOrEmpty()) {
            val cellInfo = cellInfoList[0]
            if (cellInfo is CellInfoLte) {
                return cellInfo.cellSignalStrength.level
            } else if (cellInfo is CellInfoGsm) {
                return cellInfo.cellSignalStrength.level
            } else if (cellInfo is CellInfoWcdma) {
                return cellInfo.cellSignalStrength.level
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr) { // 5G
                return cellInfo.cellSignalStrength.level
            }
        }
        return -1 // no signal
    }

    /**
     * Convert a numeric signal level (0-5) to the domain ConnectionLevel enum used by the project.
     * Any out-of-range value is interpreted as NONE.
     */
    private fun mapSignalLevel(level: Int): ConnectionLevel {

        return when (level) {
            0 -> ConnectionLevel.NONE
            1 -> ConnectionLevel.VERY_LOW
            2 -> ConnectionLevel.LOW
            3 -> ConnectionLevel.MEDIUM
            4 -> ConnectionLevel.HIGH
            5 -> ConnectionLevel.VERY_HIGH
            else -> ConnectionLevel.NONE
        }
    }

    /**
     * Checks if required telephony/location permissions have been granted.
     * This method uses Context.checkSelfPermission and thus must be executed on a Context with an appropriate lifecycle.
     */
    private fun hasPermissions(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Lightweight helper that uses the legacy activeNetworkInfo to determine availability.
     * NOTE: activeNetworkInfo is deprecated on newer APIs; consider replacing with NetworkCapabilities checks where possible.
     */
    @SuppressLint("MissingPermission")
    private fun isNetworkIsAvailableByService(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val netInfo = cm?.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }

    /**
     * Checks if the app has the ACCESS_NETWORK_STATE permission.
     */
    private fun hasAccessNetworkStatePermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
    }

}
