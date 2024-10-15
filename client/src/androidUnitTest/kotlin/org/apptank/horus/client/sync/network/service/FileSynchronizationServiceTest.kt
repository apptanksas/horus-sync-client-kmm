package org.apptank.horus.client.sync.network.service

import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.MOCK_RESPONSE_FILES_INFO
import org.apptank.horus.client.MOCK_RESPONSE_FILE_INFO
import org.apptank.horus.client.ServiceTest
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.fold
import org.apptank.horus.client.data.FileData
import org.apptank.horus.client.sync.network.dto.SyncDTO
import org.junit.Assert
import org.junit.Test

class FileSynchronizationServiceTest : ServiceTest() {

    @Test
    fun uploadFileIsSuccess() = runBlocking {
        // Given

        val referenceId = uuid()
        val file = FileData(referenceId.toByteArray(), "file.png", "image/png")

        val mockEngine = createMockResponse(MOCK_RESPONSE_FILE_INFO)
        val service = FileSynchronizationService(mockEngine, BASE_URL)

        // When
        val response = service.uploadFile(referenceId, file)

        // Then
        assert(response is DataResult.Success)
        response.fold(
            onSuccess = {
                Assert.assertNotNull(it.id)
                Assert.assertNotNull(it.url)
                Assert.assertNotNull(it.status)
                Assert.assertNotNull(it.mimeType)
            },
            onFailure = {
                assert(false)
            }
        )
    }

    @Test
    fun getFileInfoIsSuccess() = runBlocking {
        // Given

        val referenceId = uuid()

        val mockEngine = createMockResponse(MOCK_RESPONSE_FILE_INFO)
        val service = FileSynchronizationService(mockEngine, BASE_URL)

        // When
        val response = service.getFileInfo(referenceId)

        // Then
        assert(response is DataResult.Success)
        response.fold(
            onSuccess = {
                Assert.assertNotNull(it.id)
                Assert.assertNotNull(it.url)
                Assert.assertNotNull(it.status)
                Assert.assertNotNull(it.mimeType)
            },
            onFailure = {
                assert(false)
            }
        )
    }

    @Test
    fun getFilesInfoIsSuccess() = runBlocking {
        // Given

        val request = SyncDTO.Request.FilesInfoRequest(listOf(uuid()))

        val mockEngine = createMockResponse(MOCK_RESPONSE_FILES_INFO)
        val service = FileSynchronizationService(mockEngine, BASE_URL)

        // When
        val response = service.getFilesInfo(request)

        // Then
        assert(response is DataResult.Success)
        response.fold(
            onSuccess = {
                Assert.assertTrue(it.isNotEmpty())
                it.forEach {
                    Assert.assertNotNull(it.id)
                    Assert.assertNotNull(it.url)
                    Assert.assertNotNull(it.status)
                    Assert.assertNotNull(it.mimeType)
                }
            },
            onFailure = {
                assert(false)
            }
        )
    }
}