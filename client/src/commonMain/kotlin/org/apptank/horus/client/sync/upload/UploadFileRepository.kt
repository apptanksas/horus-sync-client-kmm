package org.apptank.horus.client.sync.upload

import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.absolutePath
import io.matthewnelson.kmp.file.readBytes
import io.matthewnelson.kmp.file.resolve
import io.matthewnelson.kmp.file.writeBytes
import org.apptank.horus.client.base.coFold
import io.matthewnelson.kmp.file.File as KmpFile
import org.apptank.horus.client.config.HorusConfig
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.ISyncFileDatabaseHelper
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.extensions.toFileUri
import org.apptank.horus.client.extensions.toPath
import org.apptank.horus.client.sync.network.service.IFileSynchronizationService
import org.apptank.horus.client.sync.upload.data.FileData
import org.apptank.horus.client.sync.upload.data.SyncFileResult

class UploadFileRepository(
    private val config: HorusConfig,
    private val databaseHelper: ISyncFileDatabaseHelper,
    private val service: IFileSynchronizationService
) {

    /**
     * Creates a file in the local storage and inserts it into the database.
     *
     * @param fileData The file data to create the file from.
     * @return The reference of the created file.
     */
    fun createFileLocal(fileData: FileData): Horus.FileReference {

        val fileReference = Horus.FileReference()
        val type = if (fileData.isImage()) SyncControl.FileType.IMAGE else SyncControl.FileType.FILE

        val basePathFile = getBasePathFile()
        val filename = "$fileReference.${fileData.getExtension()}"
        val urlLocal = createFileInLocalStorage(fileData, filename, basePathFile)

        databaseHelper.insert(
            SyncControl.File(
                fileReference.toString(),
                type,
                SyncControl.FileStatus.LOCAL,
                fileData.mimeType,
                urlLocal = urlLocal,
            )
        )

        return fileReference
    }

    suspend fun uploadFiles(): List<SyncFileResult> {

        val output = mutableListOf<SyncFileResult>()
        val queryResult = databaseHelper.queryByStatus(SyncControl.FileStatus.LOCAL)

        queryResult.forEach { recordFile ->
            recordFile.createFileData()?.let { fileData ->
                // Upload file to service
                service.uploadFile(recordFile.reference.toString(), fileData).coFold(
                    onSuccess = { response ->
                        val resultUpdate = databaseHelper.update(
                            SyncControl.File(
                                recordFile.reference,
                                recordFile.type,
                                SyncControl.FileStatus.SYNCED,
                                recordFile.mimeType,
                                urlRemote = response.url
                            )
                        )
                        output.add(if (resultUpdate) recordFile.reference.toSuccess() else recordFile.reference.toFailure())
                    },
                    onFailure = { _ ->
                        output.add(recordFile.reference.toFailure())
                    }
                )
            }
        }

        return output
    }


    /**
     * Get the URL of a file based on its reference.
     *
     * @param reference The reference of the file.
     * @return The URL of the file if found, `null` otherwise.
     */
    fun getImageUrl(reference: Horus.FileReference): String? {
        val file = databaseHelper.search(reference) ?: return null
        return when (file.status) {
            SyncControl.FileStatus.LOCAL -> {
                file.urlLocal
            }

            SyncControl.FileStatus.REMOTE -> {
                file.urlRemote
            }

            SyncControl.FileStatus.SYNCED -> {
                file.urlLocal
            }

            else -> {
                null
            }
        }
    }

    /**
     * Get the URL local of a file based on its reference.
     *
     * @param reference The reference of the file.
     * @return The URL of the file if found, `null` otherwise.
     */
    fun getImageUrlLocal(reference: Horus.FileReference): String? {
        return databaseHelper.search(reference)?.urlLocal
    }

    /**
     * Creates a file in the local storage and returns the URL.
     *
     * @param fileData The file data to create the file from.
     * @param filename The name of the file.
     * @param path The path to create the file in.
     * @return The URL of the created file.
     */
    private fun createFileInLocalStorage(
        fileData: FileData,
        filename: String,
        path: KmpFile
    ): String {
        val file = path.resolve(filename)
        file.writeBytes(fileData.data)
        return file.absolutePath.toFileUri()
    }

    /**
     * Get the base path for the files.
     * If the path does not exist, it will be created.
     */
    private fun getBasePathFile(): KmpFile {
        val basePath = KmpFile(normalizePath(config.baseStoragePath + HORUS_PATH_FILES))

        if (!basePath.exists()) {
            basePath.mkdirs()
        }
        return basePath
    }

    /**
     * Normalize the path to use the system directory separator.
     */
    private fun normalizePath(path: String): String {
        return path.replace("/", SysDirSep.toString())
    }

    /**
     * Get the file data based on the record file.
     *
     * @param recordFile The record file to get the data from.
     * @return The file data if found, `null` otherwise.
     */
    private fun SyncControl.File.createFileData(): FileData? {
        val absolutePathFile = urlLocal?.toPath() ?: return null
        val file = KmpFile(absolutePathFile)
        val filename = absolutePathFile.substringAfterLast("/")

        return FileData(
            file.readBytes(),
            filename,
            mimeType
        )
    }

    private fun CharSequence.toSuccess(): SyncFileResult.Success {
        return SyncFileResult.Success(Horus.FileReference(this))
    }

    private fun CharSequence.toFailure(): SyncFileResult.Failure {
        return SyncFileResult.Failure(Horus.FileReference(this))
    }

    companion object {
        private const val HORUS_PATH_FILES = "horus/sync/files/"
    }
}