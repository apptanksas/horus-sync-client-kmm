package org.apptank.horus.client.base.network

/**
 * HttpHeader is an object that defines constants for common HTTP headers.
 * These headers are used in HTTP requests to handle authentication, acting as another user,
 * content type, and accepted response formats.
 *
 * The constants defined here are commonly used in API requests.
 *
 * @author John Ospina
 * @year 2024
 */
internal object HttpHeader {
    // Header for authorization tokens
    const val AUTHORIZATION = "Authorization"

    // Custom header for indicating acting as another user
    const val USER_ACTING = "X-User-Acting"

    // Header for specifying the content type of the request body
    const val CONTENT_TYPE = "Content-Type"

    // Header for specifying the accepted content type of the response
    const val ACCEPT = "Accept"

    const val X_REQUEST_ID = "X-Request-ID"
}
