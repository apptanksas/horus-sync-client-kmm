package com.apptank.horus.client.interfaces

import com.apptank.horus.client.base.Callback

interface INetworkValidator {
    fun isNetworkAvailable(): Boolean

    fun onNetworkChange(callback: Callback)
}