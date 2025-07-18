package org.apptank.horus.client.sync.network.service

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.apptank.horus.client.MOCK_RESPONSE_FILES_INFO
import org.apptank.horus.client.MOCK_RESPONSE_FILE_INFO
import org.apptank.horus.client.ServiceTest
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.fold
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.generateFileDataImage
import org.apptank.horus.client.sync.upload.data.FileData
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

    @Test
    fun testDownloadFileIsSuccess() = runBlocking {
        // Given
        val fileReference = Horus.FileReference()
        val fileImage = generateFileDataImage()

        val mockEngine = MockEngine { _ ->
            respond(
                content = fileImage.data,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, fileImage.mimeType)
            )
        }
        val service = FileSynchronizationService(mockEngine, BASE_URL)

        // When
        val result = service.downloadFileByReferenceId(fileReference.toString())
        val result2 = service.downloadFileByUrl("https://example.com/image.png")

        // Then
        Assert.assertTrue(result is DataResult.Success)
        Assert.assertTrue(result2 is DataResult.Success)
        result.fold(
            onSuccess = {
                Assert.assertArrayEquals(fileImage.data, it.data)
                Assert.assertEquals(fileImage.mimeType, it.mimeType)
            },
            onFailure = {
                assert(false)
            }
        )
        result2.fold(
            onSuccess = {
                Assert.assertArrayEquals(fileImage.data, it.data)
            },
            onFailure = {
                assert(false)
            }
        )
    }

    @Test
    fun testDownloadFileIsFailure() = runBlocking {
        // Given
        val fileReference = Horus.FileReference()

        val mockEngine = MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val service = FileSynchronizationService(mockEngine, BASE_URL)

        // When
        val result = service.downloadFileByReferenceId(fileReference.toString())
        val result2 = service.downloadFileByUrl("https://example.com/image.png")

        // Then
        Assert.assertTrue(result is DataResult.Failure)
        Assert.assertTrue(result2 is DataResult.Failure)
    }

    @Test
    fun testWithCustomHeaders() = runBlocking {
        // Given
        val customHeaders = mapOf("Custom-Header" to "CustomValue")
        val mockEngine = createMockResponse(MOCK_RESPONSE_FILE_INFO)
        val service = FileSynchronizationService(mockEngine, BASE_URL, customHeaders)

        // When
        val response = service.uploadFile(uuid(), generateFileDataImage())

        // Then
        assert(response is DataResult.Success)
        assertRequestHeader("Custom-Header", "CustomValue")
    }
}