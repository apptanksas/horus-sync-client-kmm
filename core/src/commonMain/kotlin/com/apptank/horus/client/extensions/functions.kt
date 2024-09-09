package com.apptank.horus.client.extensions

import com.apptank.horus.client.config.TAG_LOGGING

fun log(message: String) {
    println("[Log:$TAG_LOGGING] $message\n")
}

fun info(message: String) {
    println("[Info:$TAG_LOGGING] $message\n")
}

fun logException(message: String, throwable: Throwable? = null) {
    println("[Exception:$TAG_LOGGING] $message\n")
    throwable?.printStackTrace()
}