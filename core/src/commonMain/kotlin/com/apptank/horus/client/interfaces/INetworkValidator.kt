package com.apptank.horus.client.interfaces

interface INetworkValidator {
    fun isNetworkAvailable(): Boolean

    fun onNetworkChange(callback: () -> Unit)
}