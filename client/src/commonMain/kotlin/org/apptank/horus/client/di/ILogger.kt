package org.apptank.horus.client.di

interface ILogger {
    fun log(message: String)
    fun error(message: String, throwable: Throwable?)
    fun info(message: String)
    fun warn(message: String)
}