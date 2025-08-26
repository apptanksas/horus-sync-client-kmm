package org.apptank.horus.client

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
import org.apptank.horus.client.connectivity.ConnectionLevel
import org.apptank.horus.client.connectivity.ConnectionType
import org.apptank.horus.client.connectivity.HorusClientNetworkEventBus
import org.apptank.horus.client.connectivity.INetworkValidator
import org.apptank.horus.client.connectivity.Network
import org.apptank.horus.client.connectivity.NetworkConnection
import org.apptank.horus.client.extensions.warn

internal class NetworkValidator(
    private val context: Context
) : INetworkValidator {

    private val manager: ConnectivityManager? by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private var networkChangeCallback: (() -> Unit)? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            super.onAvailable(network)
            networkChangeCallback?.invoke()
            HorusClientNetworkEventBus.emit(getNetworkInformation(context))
        }

        override fun onLost(network: android.net.Network) {
            super.onLost(network)
            networkChangeCallback?.invoke()
            HorusClientNetworkEventBus.emit(getNetworkInformation(context))
        }
    }

    @SuppressLint("MissingPermission")
    override fun isNetworkAvailable(): Boolean {
        return isNetworkIsAvailableByService() && getNetworkInfo().hasValidConnection()
    }

    override fun onNetworkChange(callback: () -> Unit) {
        networkChangeCallback = callback
        HorusClientNetworkEventBus.emit(getNetworkInformation(context))
    }

    @SuppressLint("MissingPermission")
    override fun registerNetworkCallback() {
        manager?.registerDefaultNetworkCallback(networkCallback)
    }

    override fun getNetworkInfo(): Network {
        return getNetworkInformation(context)
    }

    override fun unregisterNetworkCallback() {
        manager?.unregisterNetworkCallback(networkCallback)
    }

    private fun getNetworkInformation(context: Context): Network {
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

    private fun getWifiSignalLevel(context: Context): ConnectionLevel {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        val level = WifiManager.calculateSignalLevel(info.rssi, 5)

        return mapSignalLevel(level)
    }

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
     * Returns the mobile signal strength level (0-4) for the current cellular connection.
     * Requires the ACCESS_FINE_LOCATION permission to access cell info.
     * Returns -1 if no signal information is available.
     *
     */
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
     * Maps a signal level (0-5) to a ConnectionLevel enum.
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

    private fun hasPermissions(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isNetworkIsAvailableByService(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val netInfo = cm?.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }

}