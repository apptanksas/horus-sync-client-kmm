package com.apptank.horus.client

import android.util.Log
import com.apptank.horus.client.config.TAG_LOGGING
import com.apptank.horus.client.di.ILogger

class AndroidLogger : ILogger {

    override fun log(message: String) {
        Log.d(TAG_LOGGING, message)
    }

    override fun error(message: String, throwable: Throwable?) {
        Log.e(TAG_LOGGING, message, throwable)
    }

    override fun info(message: String) {
        Log.i(TAG_LOGGING, message)
    }

    override fun warn(message: String) {
        Log.w(TAG_LOGGING, message)
    }
}