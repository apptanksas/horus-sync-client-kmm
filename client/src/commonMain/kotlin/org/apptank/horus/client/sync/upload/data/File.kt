package org.apptank.horus.client.sync.upload.data


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

/**
 * Represents a file uploaded object containing the file ID, URL, status, and MIME type.
 *
 * @property id The ID of the file.
 * @property url The URL of the file.
 * @property status The status of the file.
 * @property mimeType The MIME type of the file.
 */
data class FileUploaded(
    val id: String,
    val url: String,
    val status: SyncFileStatus,
    val mimeType: String
)

/**
 * Represents the status of a file synchronization.
 *
 * @property id The ID of the status.
 */
enum class SyncFileStatus(val id: Int) {
    /**
     * The file is pending synchronization.
     */
    PENDING(0),

    /**
     * The file is synchronized linked with a reference id from entity data.
     */
    LINKED(1),

    /**
     * The file was deleted.
     */
    DELETED(2)
}