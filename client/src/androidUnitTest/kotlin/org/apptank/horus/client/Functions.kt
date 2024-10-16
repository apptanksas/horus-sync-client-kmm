package org.apptank.horus.client

import org.apptank.horus.client.migration.network.dto.MigrationDTO
import org.apptank.horus.client.sync.network.dto.SyncDTO
import kotlinx.serialization.json.Json
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.sync.upload.data.SyncFileStatus

private val decoder = Json { ignoreUnknownKeys = true }

internal fun buildEntitiesSchemeFromJSON(json: String): List<MigrationDTO.Response.EntityScheme> {
    return decoder.decodeFromString<List<MigrationDTO.Response.EntityScheme>>(json)
}

internal fun buildEntitiesDataFromJSON(json: String): List<SyncDTO.Response.Entity> {
    return decoder.decodeFromString<List<SyncDTO.Response.Entity>>(json)
}

internal fun generateSyncControlFile(status: SyncControl.FileStatus? = null) = SyncControl.File(
    Horus.FileReference().toString(),
    SyncControl.FileType.IMAGE,
    status ?: SyncControl.FileStatus.LOCAL,
    "image/png",
    "urlLocal",
    "urlRemote"
)

