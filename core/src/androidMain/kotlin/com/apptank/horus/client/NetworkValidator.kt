package com.apptank.horus.client

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import com.apptank.horus.client.di.INetworkValidator

class NetworkValidator(
    private val context: Context
) : INetworkValidator {

    private val manager: ConnectivityManager? by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private var networkChangeCallback: (() -> Unit)?=null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            super.onAvailable(network)
            networkChangeCallback?.invoke()
        }

        override fun onLost(network: android.net.Network) {
            super.onLost(network)
            networkChangeCallback?.invoke()
        }
    }

    @SuppressLint("MissingPermission")
    override fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val netInfo = cm?.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }

    override fun onNetworkChange(callback: () -> Unit) {
        networkChangeCallback = callback
    }

    @SuppressLint("MissingPermission")
    override fun registerNetworkCallback() {
        manager?.registerDefaultNetworkCallback(networkCallback)
    }

    override fun unregisterNetworkCallback() {
        manager?.unregisterNetworkCallback(networkCallback)
    }

}