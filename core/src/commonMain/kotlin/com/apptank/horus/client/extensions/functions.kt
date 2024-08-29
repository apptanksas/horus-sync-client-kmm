package com.apptank.horus.client.extensions

import com.apptank.horus.client.config.TAG_LOGGING

fun log(message: String) {
    print("[Log: $TAG_LOGGING] $message")
}

fun info(message: String) {
    print("[Info: $TAG_LOGGING] $message")
}

fun logException(message: String, throwable: Throwable? = null) {
    print("[Exception: $TAG_LOGGING] $message")
    throwable?.printStackTrace()
}