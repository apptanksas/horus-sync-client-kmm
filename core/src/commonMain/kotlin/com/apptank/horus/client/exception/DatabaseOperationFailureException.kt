package com.apptank.horus.client.exception

/**
 * Exception thrown when a database operation fails.
 *
 * @property message The detail message for the exception.
 */
class DatabaseOperationFailureException(
    message: String
) : Exception(message) {
}
