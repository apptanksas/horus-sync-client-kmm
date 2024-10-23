package org.apptank.horus.client.extensions

/**
 * The file scheme.
 */
private const val SCHEME_FILE = "file://"

/**
 * Converts a string uri to a path.
 *
 * @return The path.
 */
internal fun String.toPath(): String {
    return if (startsWith(SCHEME_FILE)) {
        substring(SCHEME_FILE.length)
    } else {
        this
    }
}

/**
 * Converts a string to a file uri.
 *
 * @return The file uri.
 */
fun String.toFileUri(): String {
    return if (startsWith(SCHEME_FILE)) {
        this
    } else {
        SCHEME_FILE + this
    }
}

/**
 * Normalizes a path by replacing backslashes with forward slashes.
 *
 * @return The normalized path.
 */
fun String.normalizePath(): String {
    return this.replace("\\", "/")
}