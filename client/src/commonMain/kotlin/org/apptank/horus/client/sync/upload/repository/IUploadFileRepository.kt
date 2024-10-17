package org.apptank.horus.client.sync.upload.repository

import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.sync.upload.data.FileData
import org.apptank.horus.client.sync.upload.data.SyncFileResult

interface IUploadFileRepository {
    /**
     * Creates a file in the local storage and inserts it into the database.
     *
     * @param fileData The file data to create the file from.
     * @return The reference of the created file.
     */
    fun createFileLocal(fileData: FileData): Horus.FileReference

    suspend fun uploadFiles(): List<SyncFileResult>

    /**
     * Get the URL of a file based on its reference.
     *
     * @param reference The reference of the file.
     * @return The URL of the file if found, `null` otherwise.
     */
    fun getImageUrl(reference: CharSequence): String?

    /**
     * Get the URL local of a file based on its reference.
     *
     * @param reference The reference of the file.
     * @return The URL of the file if found, `null` otherwise.
     */
    fun getImageUrlLocal(reference: CharSequence): String?
}