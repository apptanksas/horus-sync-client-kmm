package org.apptank.horus.client.sync.network.service

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.isSuccess
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.network.BaseService
import org.apptank.horus.client.base.network.HttpHeader
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.sync.upload.data.FileData
import org.apptank.horus.client.sync.network.dto.SyncDTO

/**
 * FileSynchronizationService is a service that provides methods to synchronize files with the remote server.
 *
 * The service provides methods to upload, download, and get information about files.
 *
 * @param engine The HttpClientEngine used for network requests.
 * @param baseUrl The base URL for the API endpoints.
 * @year 2024
 */
internal class FileSynchronizationService(
    engine: HttpClientEngine,
    baseUrl: String
) : BaseService(engine, baseUrl), IFileSynchronizationService {

    /**
     * Uploads a file to the remote server.
     *
     * @param referenceId The reference ID of the file.
     * @param file The file data to upload.
     * @return The result of the upload operation.
     */
    override suspend fun uploadFile(
        referenceId: String,
        file: FileData
    ): DataResult<SyncDTO.Response.FileInfoUploaded> {
        info("Uploading -> ${file.filename}")

        return postWithMultipartFormData(
            "upload/file", mapOf(
                "id" to referenceId,
                "file" to file
            ), { it.serialize() }, onProgressUpload = {
                info("Uploading --> $it")
            })
    }

    /**
     * Gets information about a file by its reference ID.
     *
     * @param referenceId The reference ID of the file.
     * @return The information about the file.
     */
    override suspend fun getFileInfo(referenceId: String): DataResult<SyncDTO.Response.FileInfoUploaded> {
        return get("upload/file/$referenceId") { it.serialize() }
    }

    /**
     * Gets information about multiple files by their reference IDs.
     *
     * @param request The request containing the reference IDs of the files.
     * @return The information about the files.
     */
    override suspend fun getFilesInfo(request: SyncDTO.Request.FilesInfoRequest): DataResult<List<SyncDTO.Response.FileInfoUploaded>> {
        return post("upload/files", request) { it.serialize() }
    }

    /**
     * Downloads a file by its reference ID.
     *
     * @param referenceId The reference ID of the file.
     * @return The file data.
     */
    override suspend fun downloadFileByReferenceId(referenceId: String): DataResult<SyncDTO.Response.FileData> {
        return downloadFile(buildUrl("wrapper/file/$referenceId"))
    }

    /**
     * Downloads a file by its URL.
     *
     * @param url The URL of the file.
     * @return The file data.
     */
    override suspend fun downloadFileByUrl(url: String): DataResult<SyncDTO.Response.FileData> {
        return downloadFile(url)
    }

    /**
     * Downloads a file by its URL.
     *
     * @param url The URL of the file.
     * @return The file data.
     */
    private suspend fun downloadFile(url: String): DataResult<SyncDTO.Response.FileData> {
        val response = client.get(url)

        return if (response.status.isSuccess()) {
            DataResult.Success(
                SyncDTO.Response.FileData(
                    response.readBytes(),
                    response.headers[HttpHeader.CONTENT_TYPE]
                        ?: throw Exception("Failed to get content type")
                )
            )
        } else {
            DataResult.Failure(Exception("Failed to download file"))
        }
    }


}