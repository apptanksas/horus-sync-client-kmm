package com.apptank.horus.client

import com.apptank.horus.client.config.TAG_LOGGING
import com.apptank.horus.client.di.ILogger
import platform.Foundation.NSLog

class IOSLogger : ILogger {

    override fun log(message: String) {
        NSLog("$TAG_LOGGING: $message")
    }

    override fun error(message: String, throwable: Throwable?) {
        if (throwable != null) {
            NSLog("$TAG_LOGGING ERROR: $message. Throwable: ${throwable.message}")
        } else {
            NSLog("$TAG_LOGGING ERROR: $message")
        }
    }

    override fun info(message: String) {
        NSLog("$TAG_LOGGING INFO: $message")
    }

    override fun warn(message: String) {
        NSLog("$TAG_LOGGING WARNING: $message")
    }
}
