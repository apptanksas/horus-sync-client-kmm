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

    /**
     * Validates if there are files to upload.
     *
     * @return `true` if there are files to upload, `false` otherwise.
     */
    fun hasFilesToUpload(): Boolean

    /**
     * Uploads all files that are in the local status.
     *
     * @return A list of [SyncFileResult] objects representing the result of the upload operation.
     */
    suspend fun uploadFiles(): List<SyncFileResult>

    /**
     * Downloads all files that are in the remote status.
     *
     * @return A list of [SyncFileResult] objects representing the result of the download operation.
     */
    suspend fun downloadRemoteFiles(): List<SyncFileResult>

    /**
     * Synchronizes the file references info between the local and remote storage.
     *
     * @return `true` if the synchronization was successful, `false` otherwise.
     */
    suspend fun syncFileReferencesInfo(): Boolean

    /**
     * Get the URL of a file based on its reference.
     *
     * @param reference The reference of the file.
     * @return The URL of the file if found, `null` otherwise.
     */
    fun getFileUrl(reference: CharSequence): String?

    /**
     * Get the URL local of a file based on its reference.
     *
     * @param reference The reference of the file.
     * @return The URL of the file if found, `null` otherwise.
     */
    fun getFileUrlLocal(reference: CharSequence): String?
}