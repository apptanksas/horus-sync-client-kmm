package org.apptank.horus.client.exception

/**
 * Exception thrown when an operation is attempted without proper user authentication.
 *
 * This exception is used to indicate that a user is not authenticated and thus cannot perform
 * certain actions that require authentication.
 */
class UserNotAuthenticatedException : HorusException("User not authenticated")
