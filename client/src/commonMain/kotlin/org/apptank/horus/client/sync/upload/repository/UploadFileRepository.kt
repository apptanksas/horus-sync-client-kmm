package org.apptank.horus.client.sync.upload.repository

import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.absolutePath
import io.matthewnelson.kmp.file.readBytes
import io.matthewnelson.kmp.file.resolve
import io.matthewnelson.kmp.file.writeBytes
import org.apptank.horus.client.base.coFold
import io.matthewnelson.kmp.file.File as KmpFile
import org.apptank.horus.client.config.HorusConfig
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.helper.ISyncFileDatabaseHelper
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.extensions.logException
import org.apptank.horus.client.extensions.toFileUri
import org.apptank.horus.client.extensions.toPath
import org.apptank.horus.client.migration.domain.AttributeType
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.IFileSynchronizationService
import org.apptank.horus.client.sync.upload.data.FileData
import org.apptank.horus.client.sync.upload.data.FileUploaded
import org.apptank.horus.client.sync.upload.data.SyncFileResult
import org.apptank.horus.client.sync.upload.data.SyncFileStatus

class UploadFileRepository(
    private val config: HorusConfig,
    private val fileDatabaseHelper: ISyncFileDatabaseHelper,
    private val controlDatabaseHelper: ISyncControlDatabaseHelper,
    private val operationDatabaseHelper: IOperationDatabaseHelper,
    private val service: IFileSynchronizationService
) : IUploadFileRepository {

    /**
     * Creates a file in the local storage and inserts it into the database.
     *
     * @param fileData The file data to create the file from.
     * @return The reference of the created file.
     */
    override fun createFileLocal(fileData: FileData): Horus.FileReference {

        val fileReference = Horus.FileReference()
        val type = if (fileData.isImage()) SyncControl.FileType.IMAGE else SyncControl.FileType.FILE

        val basePathFile = getBasePathFile()
        val filename = "$fileReference.${fileData.getExtension()}"
        val urlLocal = createFileInLocalStorage(fileData, filename, basePathFile)

        fileDatabaseHelper.insert(
            SyncControl.File(
                fileReference.toString(),
                type,
                SyncControl.FileStatus.LOCAL,
                fileData.mimeType,
                urlLocal = urlLocal,
            )
        )

        EventBus.emit(EventType.FILE_QUEUED_FOR_UPLOAD)

        return fileReference
    }

    /**
     * Uploads all files that are in the local status.
     *
     * @return A list of [SyncFileResult] indicating the result of the upload operation.
     */
    override suspend fun uploadFiles(): List<SyncFileResult> {

        val output = mutableListOf<SyncFileResult>()
        val queryResult = fileDatabaseHelper.queryByStatus(SyncControl.FileStatus.LOCAL)

        queryResult.forEach { recordFile ->
            recordFile.createFileData()?.let { fileData ->
                // Upload file to service
                service.uploadFile(recordFile.reference.toString(), fileData).coFold(
                    onSuccess = { response ->
                        val resultUpdate = fileDatabaseHelper.update(
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
     * Synchronizes the file references info between the local and remote storage.
     *
     * @return `true` if the synchronization was successful, `false` otherwise.
     */
    override suspend fun syncFileReferencesInfo(): Boolean {

        val entitiesWithRefFile =
            controlDatabaseHelper.getEntitiesWithAttributeType(AttributeType.RefFile)

        val fileReferencesFound = mutableListOf<String>()

        // 1. Get all file references from the database
        entitiesWithRefFile.forEach { entity ->
            val attributesWithRefFile =
                controlDatabaseHelper.getEntityAttributesWithType(entity, AttributeType.RefFile)

            val queryBuilder =
                SimpleQueryBuilder(entity).select(*attributesWithRefFile.toTypedArray())

            operationDatabaseHelper.queryRecords(queryBuilder).forEach { item ->
                attributesWithRefFile.forEach { attribute ->
                    item[attribute]?.let { fileReferencesFound.add(it as String) }
                }
            }
        }
        // 2. Filter file references that are not in the database
        val fileReferencesInDatabase = fileDatabaseHelper.searchBatch(fileReferencesFound)
        val fileReferencesInRemote =
            fileReferencesFound.filter { fileReferencesInDatabase.none { file -> file.reference == it } }
        var isSuccessful = false

        // 3. GET all file references status from the service and insert them into the database
        service.getFilesInfo(SyncDTO.Request.FilesInfoRequest(fileReferencesInRemote)).coFold(
            onSuccess = { response ->
                runCatching {
                    response.forEach { item ->
                        val file = item.toDomain()
                        fileDatabaseHelper.insert(
                            SyncControl.File(
                                file.id,
                                if (file.isImage()) SyncControl.FileType.IMAGE else SyncControl.FileType.FILE,
                                SyncControl.FileStatus.REMOTE,
                                file.mimeType,
                                urlRemote = file.url
                            )
                        )
                    }
                    isSuccessful = true
                }.getOrElse { e ->
                    logException("[syncFileReferences] Error inserting file references", e)
                }
            },
            onFailure = { e ->
                logException("[syncFileReferences] Error getting file references status", e)
            }
        )

        return isSuccessful
    }


    /**
     * Get the URL of a file based on its reference.
     *
     * @param reference The reference of the file.
     * @return The URL of the file if found, `null` otherwise.
     */
    override fun getImageUrl(reference: CharSequence): String? {
        val file = fileDatabaseHelper.search(reference) ?: return null
        return when (file.status) {
            SyncControl.FileStatus.LOCAL -> file.urlLocal
            SyncControl.FileStatus.REMOTE -> file.urlRemote
            SyncControl.FileStatus.SYNCED -> file.urlLocal
        }
    }

    /**
     * Get the URL local of a file based on its reference.
     *
     * @param reference The reference of the file.
     * @return The URL of the file if found, `null` otherwise.
     */
    override fun getImageUrlLocal(reference: CharSequence): String? {
        return fileDatabaseHelper.search(reference)?.urlLocal
    }

    private fun downloadFiles(filesReferences: List<FileUploaded>) {
        TODO()
    }

    /**
     * Get the URL remote of a file based on its reference.
     *
     * @param reference The reference of the file.
     * @return The URL of the file if found, `null` otherwise.
     */
    private fun SyncDTO.Response.FileInfoUploaded.toDomain(): FileUploaded {
        return FileUploaded(
            id ?: throw IllegalStateException("Id is null"),
            url ?: throw IllegalStateException("Url is null"),
            SyncFileStatus.fromId(status ?: throw IllegalStateException("Status is null")),
            mimeType ?: throw IllegalStateException("MimeType is null")
        )
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