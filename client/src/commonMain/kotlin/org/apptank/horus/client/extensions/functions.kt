package org.apptank.horus.client.extensions

import org.apptank.horus.client.di.HorusContainer

/**
 * Logs a message with a logging tag.
 *
 * This function prints the message to the standard output with a "Log" prefix and the specified tag.
 *
 * @param message The message to be logged.
 */
internal fun log(message: String) {
    HorusContainer.getLogger()?.log(message)
}

/**
 * Logs an informational message with a logging tag.
 *
 * This function prints the message to the standard output with an "Info" prefix and the specified tag.
 *
 * @param message The informational message to be logged.
 */
internal fun info(message: String) {
    HorusContainer.getLogger()?.info(message)
}

/**
 * Logs a warning message with a logging tag.
 *
 * This function prints the message to the standard output with a "Warn" prefix and the specified tag.
 *
 * @param message The warning message to be logged.
 */
internal fun warn(message: String) {
    HorusContainer.getLogger()?.warn(message)
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
    HorusContainer.getLogger()?.error(message, throwable)
}
