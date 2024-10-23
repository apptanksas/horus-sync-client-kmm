package org.apptank.horus.client

import android.net.Uri
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.normalize
import io.matthewnelson.kmp.file.toFile
import org.apptank.horus.client.migration.network.dto.MigrationDTO
import org.apptank.horus.client.sync.network.dto.SyncDTO
import kotlinx.serialization.json.Json
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.extensions.normalizePath
import org.apptank.horus.client.sync.upload.data.FileData
import org.apptank.horus.client.sync.upload.data.SyncFileStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val decoder = Json { ignoreUnknownKeys = true }

internal fun buildEntitiesSchemeFromJSON(json: String): List<MigrationDTO.Response.EntityScheme> {
    return decoder.decodeFromString<List<MigrationDTO.Response.EntityScheme>>(json)
}

internal fun buildEntitiesDataFromJSON(json: String): List<SyncDTO.Response.Entity> {
    return decoder.decodeFromString<List<SyncDTO.Response.Entity>>(json)
}

@OptIn(ExperimentalUuidApi::class)
internal fun generateSyncControlFile(
    status: SyncControl.FileStatus? = null,
    baseLocalPath: String? = null,
    id: String = Horus.FileReference().toString()
) = SyncControl.File(
    id,
    SyncControl.FileType.IMAGE,
    status ?: SyncControl.FileStatus.LOCAL,
    "image/png",
    "file://${baseLocalPath?.also { if (it.endsWith("/")) it else it.substring(-1) } ?: "test/"}${Uuid.random()}.png".normalizePath(),
    "http://test/${Uuid.random()}.png"
)

internal fun generateFileDataImage() = FileData(
    byteArrayOf(0, 1, 2, 3),
    "file.png",
    "image/png"
)

