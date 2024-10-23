package org.apptank.horus.client.exception

/**
 * Exception thrown when a file exceeds the maximum allowed size.
 *
 * @param maxSize The maximum allowed size in bytes.
 */
class FileSizeExceededException(maxSize: Int) : Exception(
    "The file size exceeds the maximum allowed. Max size: $maxSize bytes."
)