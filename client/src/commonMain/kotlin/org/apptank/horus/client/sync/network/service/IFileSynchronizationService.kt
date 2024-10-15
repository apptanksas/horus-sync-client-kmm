package org.apptank.horus.client.sync.network.service

import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.data.FileData
import org.apptank.horus.client.sync.network.dto.SyncDTO

/**
 * Interface defining the file synchronization service operations.
 */
interface IFileSynchronizationService {

    /**
     * Uploads a file to the server.
     *
     * @param referenceId The reference identifier for the file.
     * @param file The [FileData] object containing the file data.
     * @return [DataResult] containing [SyncDTO.Response.FileInfoUploaded] if successful.
     */
    suspend fun uploadFile(
        referenceId: String,
        file: FileData
    ): DataResult<SyncDTO.Response.FileInfoUploaded>

    /**
     * Retrieves information about a file from the server.
     *
     * @param referenceId The reference identifier for the file.
     * @return [DataResult] containing [SyncDTO.Response.FileInfoUploaded] if successful.
     */
    suspend fun getFileInfo(
        referenceId: String
    ): DataResult<SyncDTO.Response.FileInfoUploaded>


    /**
     * Retrieves information about multiple files from the server.
     *
     * @param request [SyncDTO.Request.FilesInfoRequest] containing the reference identifiers for the files.
     * @return [DataResult] containing a list of [SyncDTO.Response.FileInfoUploaded] if successful.
     */
    suspend fun getFilesInfo(
        request: SyncDTO.Request.FilesInfoRequest
    ): DataResult<List<SyncDTO.Response.FileInfoUploaded>>

}