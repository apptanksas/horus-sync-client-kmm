package org.apptank.horus.client.data


/**
 * Represents a file data object containing the file data, filename, and MIME type.
 *
 * @property data The byte array representing the file data.
 * @property filename The name of the file.
 * @property mimeType The MIME type of the file.
 */
data class FileData(
    val data: ByteArray,
    val filename: String,
    val mimeType: String
)