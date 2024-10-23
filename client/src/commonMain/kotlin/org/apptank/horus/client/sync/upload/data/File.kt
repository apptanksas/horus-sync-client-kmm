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
) {
    /**
     * Checks if the file is an image based on the MIME type.
     *
     * @return `true` if the file is an image, `false` otherwise.
     */
    fun isImage(): Boolean {
        return mimeType.startsWith("image/")
    }

    /**
     * Gets the extension of the file.
     *
     * @return The extension of the file.
     */
    fun getExtension(): String {
        return filename.substringAfterLast(".")
    }

    /**
     * Gets the MIME type of the file.
     *
     * @return The MIME type of the file.
     */
    fun getMimeType(): FileMimeType {

        val mimeType = FileMimeType.fromType(mimeType)

        if (mimeType != null) {
            return mimeType
        }

        FileMimeType.fromExtension(getExtension())?.let {
            return it
        }

        throw IllegalArgumentException("Invalid MIME type")
    }

    /**
     * Gets the size of the file data.
     *
     * @return The size of the file data.
     */
    fun getSize(): Int {
        return data.size
    }
}

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
) {
    /**
     * Checks if the file is an image based on the MIME type.
     *
     * @return `true` if the file is an image, `false` otherwise.
     */
    fun isImage(): Boolean {
        return mimeType.startsWith("image/")
    }
}

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
    DELETED(2);

    companion object {
        /**
         * Gets the [SyncFileStatus] based on the ID.
         *
         * @param id The ID of the status.
         * @return The [SyncFileStatus] if found, `null` otherwise.
         */
        fun fromId(id: Int): SyncFileStatus {
            return entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid ID")
        }
    }
}


sealed class SyncFileResult {
    data class Success(val fileReference: CharSequence) : SyncFileResult()
    data class Failure(val fileReference: CharSequence, val exception: Throwable) : SyncFileResult()
}

