package org.apptank.horus.client.sync.network.service

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.isSuccess
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.network.BaseService
import org.apptank.horus.client.base.network.HttpHeader
import org.apptank.horus.client.sync.upload.data.FileData
import org.apptank.horus.client.sync.network.dto.SyncDTO

internal class FileSynchronizationService(
    engine: HttpClientEngine,
    baseUrl: String
) : BaseService(engine, baseUrl), IFileSynchronizationService {

    override suspend fun uploadFile(
        referenceId: String,
        file: FileData
    ): DataResult<SyncDTO.Response.FileInfoUploaded> {
        return postWithMultipartFormData(
            "upload/file", mapOf(
                "id" to referenceId,
                "file" to file
            ), { it.serialize() })
    }

    override suspend fun getFileInfo(referenceId: String): DataResult<SyncDTO.Response.FileInfoUploaded> {
        return get("upload/file/$referenceId") { it.serialize() }
    }

    override suspend fun getFilesInfo(request: SyncDTO.Request.FilesInfoRequest): DataResult<List<SyncDTO.Response.FileInfoUploaded>> {
        return post("upload/files", request) { it.serialize() }
    }

    override suspend fun downloadFileByReferenceId(referenceId: String): DataResult<SyncDTO.Response.FileData> {
        return downloadFile(buildUrl("wrapper/file/$referenceId"))
    }

    override suspend fun downloadFileByUrl(url: String): DataResult<SyncDTO.Response.FileData> {
        return downloadFile(url)
    }

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