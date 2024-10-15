package org.apptank.horus.client.sync.network.service

import io.ktor.client.engine.HttpClientEngine
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.network.BaseService
import org.apptank.horus.client.data.FileData
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
            )
        ) { it.serialize() }
    }

    override suspend fun getFileInfo(referenceId: String): DataResult<SyncDTO.Response.FileInfoUploaded> {
        return get("upload/file/$referenceId") { it.serialize() }
    }

    override suspend fun getFilesInfo(request: SyncDTO.Request.FilesInfoRequest): DataResult<List<SyncDTO.Response.FileInfoUploaded>> {
        return post("upload/files", request) { it.serialize() }
    }

}