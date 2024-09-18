package com.apptank.horus.client.migration.exception

/**
 * Exception thrown when an invalid data scheme is encountered.
 *
 * This exception is used to indicate errors related to data schemes that do not meet the expected criteria or have invalid configurations.
 *
 * @param message A description of the error that caused the exception. Defaults to an empty string.
 * @constructor Creates a new instance of `InvalidDataSchemeException` with the specified error message.
 */
class InvalidDataSchemeException(message: String = "") : Exception(message)
