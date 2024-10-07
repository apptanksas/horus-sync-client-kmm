package org.apptank.horus.client

import org.apptank.horus.client.di.ILogger

class KotlinLogger:ILogger {

    override fun log(message: String) {
        println("Log: $message")
    }

    override fun error(message: String, throwable: Throwable?) {
        println("Error: $message")
        throwable?.printStackTrace()
    }

    override fun info(message: String) {
        println("Info: $message")
    }

    override fun warn(message: String) {
        println("Warn: $message")
    }
}