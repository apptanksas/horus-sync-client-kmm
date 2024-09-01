package com.apptank.horus.client.sync.network.service

import com.apptank.horus.client.MOCK_RESPONSE_GET_DATA
import com.apptank.horus.client.MOCK_RESPONSE_GET_DATA_ENTITY
import com.apptank.horus.client.ServiceTest
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.base.fold
import com.apptank.horus.client.control.SyncActionType
import com.apptank.horus.client.sync.network.dto.SyncActionRequest
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test


class SynchronizationServiceTest : ServiceTest() {

    @Test
    fun getDataIsSuccess() = runBlocking {
        // Given
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_DATA)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getData()
        // Then
        assert(response is DataResult.Success)
        response.fold(
            onSuccess = {
                Assert.assertEquals(2, it.size)
            },
            onFailure = {
                Assert.fail("Error")
            }
        )
    }

    @Test
    fun getDataIsFailure() = runBlocking {
        // Given
        val mockEngine = createMockResponse("{}", status = HttpStatusCode.InternalServerError)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getData()
        // Then
        assert(response is DataResult.Failure)
    }

    @Test
    fun getDataEntityIsSuccess() = runBlocking {
        // Given
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_DATA_ENTITY)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getDataEntity("farms")
        // Then
        assert(response is DataResult.Success)
        response.fold(
            onSuccess = {
                Assert.assertEquals(1, it.size)
            },
            onFailure = {
                Assert.fail("Error")
            }
        )
    }

    @Test
    fun getDataEntityWithIds() = runBlocking {
        // Given
        val ids = generateArray { uuid() }.map { it }
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_DATA_ENTITY)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getDataEntity("farms", ids)
        // Then
        assert(response is DataResult.Success)
        assertRequestQueryParams("ids", ids.joinToString(","))
    }

    @Test
    fun postQueueActions() = runBlocking {

        // Given
        val actions = generateArray {
            SyncActionRequest(
                SyncActionType.INSERT.name, "farms", mapOf(
                    "id" to uuid(),
                    "name" to "Farm ${uuid()}"
                ), timestamp()
            )
        }
        val mockEngine = createMockResponse(status = HttpStatusCode.Created)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.postQueue(actions)
        // Then
        assert(response is DataResult.Success)
        assertRequestBody(Json.encodeToString(actions))
    }
}