package org.apptank.horus.client.sync.upload.repository

import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.every
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.TestCase
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.config.HorusConfig
import org.apptank.horus.client.config.UploadFilesConfig
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.helper.ISyncFileDatabaseHelper
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.exception.FileMimeTypeNotAllowedException
import org.apptank.horus.client.exception.FileSizeExceededException
import org.apptank.horus.client.extensions.toPath
import org.apptank.horus.client.generateFileDataImage
import org.apptank.horus.client.generateSyncControlFile
import org.apptank.horus.client.migration.domain.AttributeType
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.IFileSynchronizationService
import org.apptank.horus.client.sync.upload.data.FileData
import org.apptank.horus.client.sync.upload.data.FileMimeType
import org.apptank.horus.client.sync.upload.data.SyncFileResult
import org.apptank.horus.client.sync.upload.data.SyncFileStatus
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random


class UploadFileRepositoryTest : TestCase() {

    @Mock
    private val controlDatabaseHelper: ISyncControlDatabaseHelper =
        mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    private val operationDatabaseHelper: IOperationDatabaseHelper =
        mock(classOf<IOperationDatabaseHelper>())

    @Mock
    private val fileDatabaseHelper: ISyncFileDatabaseHelper =
        mock(classOf<ISyncFileDatabaseHelper>())

    @Mock
    private val service: IFileSynchronizationService = mock(classOf<IFileSynchronizationService>())

    private lateinit var repository: UploadFileRepository

    @Before
    fun setUp() {
        repository = UploadFileRepository(
            getHorusConfigTest(),
            fileDatabaseHelper, controlDatabaseHelper, operationDatabaseHelper, service
        )
    }

    @Test
    fun testCreateFileLocalIsSuccess() {
        // Given
        val fileData = generateFileDataImage()

        // When
        val result = repository.createFileLocal(fileData)

        // Then
        // Validate if is a UUID
        Assert.assertTrue(
            result.toString()
                .matches(Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))
        )
        verify { fileDatabaseHelper.insert(any()) }.wasInvoked()
    }

    @Test
    fun testCreateFileLocalIsFailureByMimeTypeNotAllowed() {
        // Given
        val fileDataVideo = FileData(
            byteArrayOf(0, 1, 2, 3),
            "file." + FileMimeType.VIDEO_MPEG_4_VIDEO.extension,
            FileMimeType.VIDEO_MPEG_4_VIDEO.type
        )

        // When
        Assert.assertThrows(FileMimeTypeNotAllowedException::class.java) {
            repository.createFileLocal(fileDataVideo)
        }
    }

    @Test
    fun testCreateFileLocalIsFailureBySizeExceeded() {
        // Given
        val repository = UploadFileRepository(
            HorusConfig(
                "http://test",
                UploadFilesConfig("test", listOf(FileMimeType.IMAGE_PORTABLE_NETWORK_GRAPHICS), 1)
            ),
            fileDatabaseHelper, controlDatabaseHelper, operationDatabaseHelper, service
        )
        val fileData = FileData(
            byteArrayOf(0, 1, 2, 3),
            "file.png",
            "image/png"
        )

        // When
        Assert.assertThrows(FileSizeExceededException::class.java) {
            repository.createFileLocal(fileData)
        }
    }

    @Test
    fun testGetImageUrlWhenIsRemoteIsSuccess() {
        // Given
        val fileReference = Horus.FileReference()
        val recordFile = generateSyncControlFile(SyncControl.FileStatus.REMOTE)

        every { fileDatabaseHelper.search(fileReference) }.returns(recordFile)

        // When
        val result = repository.getImageUrl(fileReference)

        // Then
        Assert.assertEquals(recordFile.urlRemote, result)
    }

    @Test
    fun testGetImageUrlWhenIsLocalIsSuccess() {
        // Given
        val fileReference = Horus.FileReference()
        val recordFile = generateSyncControlFile(SyncControl.FileStatus.LOCAL)

        every { fileDatabaseHelper.search(fileReference) }.returns(recordFile)

        // When
        val result = repository.getImageUrl(fileReference)

        // Then
        Assert.assertEquals(recordFile.urlLocal, result)
    }

    @Test
    fun testGetImageUrlWhenIsSyncedIsSuccess() {
        // Given
        val fileReference = Horus.FileReference()
        val recordFile = generateSyncControlFile(SyncControl.FileStatus.SYNCED)

        every { fileDatabaseHelper.search(fileReference) }.returns(recordFile)

        // When
        val result = repository.getImageUrl(fileReference)

        // Then
        Assert.assertEquals(recordFile.urlLocal, result)
    }

    @Test
    fun testGetImageUrlIsNull() {
        // Given
        val fileReference = Horus.FileReference()

        every { fileDatabaseHelper.search(fileReference) }.returns(null)

        // When
        val result = repository.getImageUrl(fileReference)

        // Then
        Assert.assertNull(result)
    }

    @Test
    fun testGetImageUrlLocalIsSuccess() {
        // Given
        val fileReference = Horus.FileReference()
        val recordFile = generateSyncControlFile(SyncControl.FileStatus.LOCAL)

        every { fileDatabaseHelper.search(fileReference) }.returns(recordFile)

        // When
        val result = repository.getImageUrlLocal(fileReference)

        // Then
        Assert.assertEquals(recordFile.urlLocal, result)
    }

    @Test
    fun testGetImageUrlLocalIsNull() {
        // Given
        val fileReference = Horus.FileReference()

        every { fileDatabaseHelper.search(fileReference) }.returns(null)

        // When
        val result = repository.getImageUrlLocal(fileReference)

        // Then
        Assert.assertNull(result)
    }

    @Test
    fun testUploadFilesIsSuccess() = runBlocking {
        // Given
        val recordFiles = generateArray {
            generateSyncControlFile(SyncControl.FileStatus.LOCAL, getLocalTestPath()).also {
                it.urlLocal?.let {
                    createFileInLocalStorage(it.toPath())
                }
            }
        }
        val fileResponse = SyncDTO.Response.FileInfoUploaded()

        every { fileDatabaseHelper.queryByStatus(SyncControl.FileStatus.LOCAL) }.returns(recordFiles)
        coEvery { service.uploadFile(any(), any()) }.returns(DataResult.Success(fileResponse))
        every { fileDatabaseHelper.update(any()) }.returns(true)

        // When
        val result = repository.uploadFiles()

        // Then
        Assert.assertEquals(recordFiles.size, result.size)
        Assert.assertTrue(result.all { it is SyncFileResult.Success })
    }

    @Test
    fun testSyncFileReferencesIsSuccess() = runBlocking {
        // Given
        val entities = generateRandomArray { "entity_" + Random.nextInt(1, 999999) }
        val columns = generateRandomArray { "column_" + Random.nextInt(1, 999999) }
        val fileReferencesFound = generateArray {
            mutableMapOf<String, String>().apply {
                columns.forEach {
                    put(it, Horus.FileReference().toString())
                }
            }
        }
        val fileReferencesInDatabase = generateRandomArray(fileReferencesFound.size) {
            generateSyncControlFile(id = fileReferencesFound.random().values.first())
        }.distinctBy { it.reference }

        val countFileReferencesToInsert = fileReferencesFound.size - fileReferencesInDatabase.size

        val filesFromServices = generateArray(countFileReferencesToInsert) {
            SyncDTO.Response.FileInfoUploaded(
                Horus.FileReference().toString(),
                "http://test/${Horus.FileReference()}.png",
                "image/png",
                SyncFileStatus.LINKED.id
            )
        }

        every { controlDatabaseHelper.getEntitiesWithAttributeType(AttributeType.RefFile) }
            .returns(entities)
        every { controlDatabaseHelper.getEntityAttributesWithType(any(), any()) }
            .returns(columns)
        every { operationDatabaseHelper.queryRecords(any()) }.returns(fileReferencesFound)
        every { fileDatabaseHelper.searchBatch(any()) }.returns(fileReferencesInDatabase)
        coEvery { service.getFilesInfo(any()) }.returns(DataResult.Success(filesFromServices))

        // When
        val result = repository.syncFileReferencesInfo()

        // Then
        verify { fileDatabaseHelper.insert(any()) }.wasInvoked(countFileReferencesToInsert)
        Assert.assertTrue(result)
    }

    @Test
    fun testSyncFileReferencesIsFailureByService() = runBlocking {
        // Given
        val entities = generateRandomArray(2) { "entity_" + Random.nextInt(1, 999999) }
        val columns = generateRandomArray(2) { "column_" + Random.nextInt(1, 999999) }
        val fileReferencesFound = generateArray(100) {
            mutableMapOf<String, String>().apply {
                columns.forEach {
                    put(it, Horus.FileReference().toString())
                }
            }
        }
        every { controlDatabaseHelper.getEntitiesWithAttributeType(AttributeType.RefFile) }
            .returns(entities)
        every { controlDatabaseHelper.getEntityAttributesWithType(any(), any()) }
            .returns(columns)
        every { operationDatabaseHelper.queryRecords(any()) }.returns(fileReferencesFound)
        every { fileDatabaseHelper.searchBatch(any()) }.returns(emptyList())
        coEvery { service.getFilesInfo(any()) }.returns(DataResult.Failure(Exception()))

        // When
        val result = repository.syncFileReferencesInfo()

        // Then
        verify { fileDatabaseHelper.insert(any()) }.wasNotInvoked()
        Assert.assertFalse(result)
    }

    @Test
    fun testSyncFileReferencesIsFailureByInsert() = runBlocking {
        // Given
        val entities = generateRandomArray(2) { "entity_" + Random.nextInt(1, 999999) }
        val columns = generateRandomArray(2) { "column_" + Random.nextInt(1, 999999) }
        val fileReferencesFound = generateArray(100) {
            mutableMapOf<String, String>().apply {
                columns.forEach {
                    put(it, Horus.FileReference().toString())
                }
            }
        }
        every { controlDatabaseHelper.getEntitiesWithAttributeType(AttributeType.RefFile) }
            .returns(entities)
        every { controlDatabaseHelper.getEntityAttributesWithType(any(), any()) }
            .returns(columns)
        every { operationDatabaseHelper.queryRecords(any()) }.returns(fileReferencesFound)
        every { fileDatabaseHelper.searchBatch(any()) }.returns(emptyList())
        coEvery { service.getFilesInfo(any()) }.returns(DataResult.Success(
            generateArray {
                SyncDTO.Response.FileInfoUploaded(
                    Horus.FileReference().toString(),
                    "http://test/${Horus.FileReference()}.png",
                    "image/png",
                    SyncFileStatus.LINKED.id
                )
            }
        ))
        every { fileDatabaseHelper.insert(any()) }.throws(Exception())

        // When
        val result = repository.syncFileReferencesInfo()

        // Then
        Assert.assertFalse(result)
    }

    @Test
    fun testDownloadFilesIsSuccess() = runBlocking {
        val filesUploadedInRemote =
            generateRandomArray { generateSyncControlFile(SyncControl.FileStatus.REMOTE) }
        val fileDownloaded = SyncDTO.Response.FileData(byteArrayOf(0, 1, 2, 3), "image/png")

        every { fileDatabaseHelper.queryByStatus(SyncControl.FileStatus.REMOTE) }.returns(
            filesUploadedInRemote
        )
        coEvery { service.downloadFileByUrl(any()) }.returns(DataResult.Success(fileDownloaded))
        every { fileDatabaseHelper.update(any()) }.returns(true)

        // When
        val result = repository.downloadRemoteFiles()

        // Then
        verify { fileDatabaseHelper.update(any()) }.wasInvoked(filesUploadedInRemote.size)
        Assert.assertEquals(
            filesUploadedInRemote.size,
            result.filter { it is SyncFileResult.Success }.size
        )
        Assert.assertEquals(0, result.filter { it is SyncFileResult.Failure }.size)
    }
}