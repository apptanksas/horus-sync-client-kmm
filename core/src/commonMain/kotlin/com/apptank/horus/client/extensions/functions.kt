package com.apptank.horus.client.extensions

import com.apptank.horus.client.config.TAG_LOGGING

/**
 * Logs a message with a logging tag.
 *
 * This function prints the message to the standard output with a "Log" prefix and the specified tag.
 *
 * @param message The message to be logged.
 */
fun log(message: String) {
    println("[Log:$TAG_LOGGING] $message\n")
}

/**
 * Logs an informational message with a logging tag.
 *
 * This function prints the message to the standard output with an "Info" prefix and the specified tag.
 *
 * @param message The informational message to be logged.
 */
fun info(message: String) {
    println("[Info:$TAG_LOGGING] $message\n")
}


/**
 * Logs an exception message with a logging tag.
 *
 * This function prints the exception message to the standard output with an "Exception" prefix and the specified tag.
 * It also prints the stack trace of the exception if provided.
 *
 * @param message The message describing the exception.
 * @param throwable The exception whose stack trace should be printed (optional).
 */
fun logException(message: String, throwable: Throwable? = null) {
    println("[Exception:$TAG_LOGGING] $message\n")
    throwable?.printStackTrace()
}
