package org.apptank.horus.client.sync.upload

import org.apptank.horus.client.config.HorusConfig
import org.apptank.horus.client.control.helper.ISyncFileDatabaseHelper
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.sync.network.service.IFileSynchronizationService
import org.apptank.horus.client.sync.upload.data.FileData

class UploadFileRepository(
    private val config: HorusConfig,
    private val databaseHelper: ISyncFileDatabaseHelper,
    private val service: IFileSynchronizationService
) {

    fun createFileReference(fileData: FileData): Horus.FileReference {
        TODO()
    }
}