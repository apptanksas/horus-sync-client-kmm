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
import org.apptank.horus.client.exception.FileMimeTypeNotAllowedException
import org.apptank.horus.client.exception.FileSizeExceededException
import org.apptank.horus.client.extensions.info
import org.apptank.horus.client.extensions.logException
import org.apptank.horus.client.extensions.toFileUri
import org.apptank.horus.client.extensions.toPath
import org.apptank.horus.client.migration.domain.AttributeType
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.IFileSynchronizationService
import org.apptank.horus.client.sync.upload.data.FileData
import org.apptank.horus.client.sync.upload.data.FileMimeType
import org.apptank.horus.client.sync.upload.data.FileUploaded
import org.apptank.horus.client.sync.upload.data.SyncFileResult
import org.apptank.horus.client.sync.upload.data.SyncFileStatus

/**
 * UploadFileRepository is responsible for creating, uploading, and downloading files.
 *
 * The repository interacts with the local and remote storage to manage file operations.
 *
 * @param config The Horus configuration.
 * @param fileDatabaseHelper The helper for the file database.
 * @param controlDatabaseHelper The helper for the control database.
 * @param operationDatabaseHelper The helper for the operation database.
 * @param service The service for file synchronization.
 * @constructor Creates a repository with the required dependencies.
 */
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

        fileData.getMimeType().let {
            // Validate if the MIME type is allowed
            if (!config.uploadFilesConfig.mimeTypesAllowed.contains(it)) {
                throw FileMimeTypeNotAllowedException(it)
            }
        }

        // Validate if the file size is exceeded
        if (fileData.data.size > config.uploadFilesConfig.maxFileSize) {
            throw FileSizeExceededException(config.uploadFilesConfig.maxFileSize)
        }

        val fileReference = Horus.FileReference()
        val type = if (fileData.isImage()) SyncControl.FileType.IMAGE else SyncControl.FileType.FILE

        val basePathFile = getBasePathFile()
        val filename = "$fileReference.${fileData.getExtension()}"
        val urlLocal = createFileInLocalStorage(fileData.data, filename, basePathFile)

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
     * Validates if there are files to upload.
     *
     * @return `true` if there are files to upload, `false` otherwise.
     */
    override fun hasFilesToUpload(): Boolean {
        return fileDatabaseHelper.queryByStatus(SyncControl.FileStatus.LOCAL).isNotEmpty()
    }


    /**
     * Uploads all files that are in the local status.
     *
     * @return A list of [SyncFileResult] indicating the result of the upload operation.
     */
    override suspend fun uploadFiles(): List<SyncFileResult> {

        val output = mutableListOf<SyncFileResult>()
        val queryResult = fileDatabaseHelper.queryByStatus(SyncControl.FileStatus.LOCAL)

        info("Upload: ${queryResult.size} files")

        queryResult.forEach { recordFile ->

            runCatching {
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
                                    urlLocal = recordFile.urlLocal,
                                    urlRemote = response.url
                                )
                            )
                            output.add(
                                if (resultUpdate) recordFile.reference.toSuccess() else recordFile.reference.toFailure(
                                    Exception("Error updating file")
                                )
                            )
                        },
                        onFailure = { e ->
                            output.add(recordFile.reference.toFailure(e))
                        }
                    )
                }?:{
                    output.add(recordFile.reference.toFailure(Exception("Error file not found -> ${recordFile.urlLocal}")))
                }
            }.getOrElse { e ->
                output.add(recordFile.reference.toFailure(e))
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

        if (fileReferencesInRemote.isEmpty()) {
            return true
        }

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
     * Downloads all files that are in the remote status.
     *
     * @return A list of [SyncFileResult] indicating the result of the download operation.
     */
    override suspend fun downloadRemoteFiles(): List<SyncFileResult> {

        val result = mutableListOf<SyncFileResult>()

        fileDatabaseHelper.queryByStatus(SyncControl.FileStatus.REMOTE).forEach { recordFile ->
            runCatching {

                recordFile.urlRemote?.let { url ->

                    service.downloadFileByUrl(url).coFold(
                        onSuccess = { fileData ->
                            val mimeType =
                                FileMimeType.fromType(fileData.mimeType)
                                    ?: throw IllegalStateException(
                                        "Mime type not found"
                                    )
                            val filename = "${recordFile.reference}.${mimeType.extension}"
                            val fileLocalUri =
                                createFileInLocalStorage(fileData.data, filename, getBasePathFile())

                            val updateResult = fileDatabaseHelper.update(
                                SyncControl.File(
                                    recordFile.reference,
                                    recordFile.type,
                                    SyncControl.FileStatus.SYNCED,
                                    mimeType.type,
                                    urlLocal = fileLocalUri,
                                    urlRemote = recordFile.urlRemote
                                )
                            )

                            result.add(
                                if (updateResult) recordFile.reference.toSuccess() else recordFile.reference.toFailure(
                                    Exception("Error updating file")
                                )
                            )
                        },
                        onFailure = { e ->
                            result.add(
                                SyncFileResult.Failure(
                                    recordFile.reference,
                                    e
                                )
                            )
                        })
                }
            }.getOrElse {
                result.add(recordFile.reference.toFailure(it))
            }
        }

        return result
    }


    /**
     * Get the URL of a file based on its reference.
     *
     * @param reference The reference of the file.
     * @return The URL of the file if found, `null` otherwise.
     */
    override fun getFileUrl(reference: CharSequence): String? {
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
    override fun getFileUrlLocal(reference: CharSequence): String? {
        return fileDatabaseHelper.search(reference)?.urlLocal
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
     * @param data The data to create the file from.
     * @param filename The name of the file.
     * @param path The path to create the file in.
     * @return The URL of the created file.
     */
    private fun createFileInLocalStorage(
        data: ByteArray,
        filename: String,
        path: KmpFile
    ): String {
        val file = path.resolve(filename)
        file.writeBytes(data)
        return file.absolutePath.toFileUri()
    }

    /**
     * Get the base path for the files.
     * If the path does not exist, it will be created.
     */
    private fun getBasePathFile(): KmpFile {
        val basePath =
            KmpFile(normalizePath(config.uploadFilesConfig.baseStoragePath + HORUS_PATH_FILES))

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
        var absolutePathFile = urlLocal?.toPath() ?: return null
        var file = KmpFile(absolutePathFile)

        if (file.exists().not()) {
            absolutePathFile = absolutePathFile.fallbackFileLocalUri()
            file = KmpFile(absolutePathFile)
        }

        if (file.exists().not()) {
            error("[createFileData] File doesn't exists -> $absolutePathFile")
            return null
        }

        val filename = absolutePathFile.substringAfterLast("/")

        return FileData(
            file.readBytes(),
            filename,
            mimeType
        )
    }

    internal fun String.fallbackFileLocalUri(): String {
        return config.uploadFilesConfig.baseStoragePath+this.substring(this.indexOf(HORUS_PATH_FILES))
    }

    private fun CharSequence.toSuccess(): SyncFileResult.Success {
        return SyncFileResult.Success(Horus.FileReference(this))
    }

    private fun CharSequence.toFailure(e: Throwable): SyncFileResult.Failure {
        return SyncFileResult.Failure(Horus.FileReference(this), e)
    }

    internal companion object {
        const val HORUS_PATH_FILES = "horus/sync/files/"
    }
}