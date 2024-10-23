package org.apptank.horus.client.exception

import org.apptank.horus.client.sync.upload.data.FileMimeType

/**
 * Exception thrown when the file MIME type is not allowed.
 *
 * @property mimeType The file MIME type.
 */
class FileMimeTypeNotAllowedException(mimeType: FileMimeType) : Exception(
    "The file MIME type [${mimeType.type}] is not allowed."
)