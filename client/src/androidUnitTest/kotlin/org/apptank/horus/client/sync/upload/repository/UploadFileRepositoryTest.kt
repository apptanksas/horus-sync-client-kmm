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
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.helper.ISyncFileDatabaseHelper
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.extensions.toPath
import org.apptank.horus.client.generateFileDataImage
import org.apptank.horus.client.generateSyncControlFile
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.apptank.horus.client.sync.network.service.IFileSynchronizationService
import org.apptank.horus.client.sync.upload.data.SyncFileResult
import org.junit.Assert
import org.junit.Before
import org.junit.Test


class UploadFileRepositoryTest : TestCase() {

    @Mock
    private val controlDatabaseHelper: ISyncControlDatabaseHelper =
        mock(classOf<ISyncControlDatabaseHelper>())

    @Mock
    private val fileDatabaseHelper: ISyncFileDatabaseHelper =
        mock(classOf<ISyncFileDatabaseHelper>())

    @Mock
    private val service: IFileSynchronizationService = mock(classOf<IFileSynchronizationService>())

    private lateinit var repository: UploadFileRepository

    @Before
    fun setUp() {
        repository = UploadFileRepository(
            HorusConfig("http://dev.api", getLocalTestPath()),
            fileDatabaseHelper, controlDatabaseHelper, service
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

}