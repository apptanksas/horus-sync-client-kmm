package com.apptank.horus.client.sync.network.service

import com.apptank.horus.client.MOCK_RESPONSE_GET_DATA
import com.apptank.horus.client.MOCK_RESPONSE_GET_DATA_ENTITY
import com.apptank.horus.client.MOCK_RESPONSE_GET_QUEUE_ACTIONS
import com.apptank.horus.client.MOCK_RESPONSE_POST_VALIDATE_DATA
import com.apptank.horus.client.MOCK_RESPONSE_POST_VALIDATE_HASHING
import com.apptank.horus.client.ServiceTest
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.base.fold
import com.apptank.horus.client.control.SyncActionType
import com.apptank.horus.client.hashing.SyncHasher
import com.apptank.horus.client.sync.network.dto.EntityHash
import com.apptank.horus.client.sync.network.dto.SyncActionRequest
import com.apptank.horus.client.sync.network.dto.ValidateHashingRequest
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
                it.forEach {
                    Assert.assertNotNull(it.entity)
                    Assert.assertFalse(it.data?.isEmpty() ?: true)
                }
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
        assertRequestContainsQueryParam("ids", ids.joinToString(","))
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
        val response = service.postQueueActions(actions)
        // Then
        assert(response is DataResult.Success)
        assertRequestBody(Json.encodeToString(actions))
    }

    @Test
    fun getQueueActionsDefault() = runBlocking {
        // Given
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_QUEUE_ACTIONS)
        val service = SynchronizationService(mockEngine, BASE_URL)
        val countExpected = 7
        // When
        val response = service.getQueueActions()
        // Then
        assert(response is DataResult.Success)
        response.fold(
            onSuccess = {
                Assert.assertEquals(countExpected, it.size)
                it.forEach {
                    Assert.assertNotNull(it.action)
                    Assert.assertNotNull(it.entity)
                    Assert.assertNotNull(it.data)
                    Assert.assertFalse(it.data?.isEmpty() ?: true)
                    Assert.assertNotNull(it.actionedAt)
                    Assert.assertNotNull(it.syncedAt)
                }
            },
            onFailure = {
                Assert.fail("Error")
            }
        )
        assertRequestMissingQueryParam("after")
        assertRequestMissingQueryParam("exclude")
    }

    @Test
    fun getQueueActionsWithTimestampAfter() = runBlocking {
        // Given
        val timestampAfter = timestamp()
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_QUEUE_ACTIONS)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getQueueActions(timestampAfter)
        // Then
        assert(response is DataResult.Success)
        assertRequestContainsQueryParam("after", timestampAfter.toString())
        assertRequestMissingQueryParam("exclude")
    }

    @Test
    fun getQueueActionsWithExclude() = runBlocking {
        // Given
        val exclude = generateArray { timestamp() }
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_QUEUE_ACTIONS)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getQueueActions(exclude = exclude)
        // Then
        assert(response is DataResult.Success)
        assertRequestContainsQueryParam("exclude", exclude.joinToString(","))
        assertRequestMissingQueryParam("after")
    }

    @Test
    fun getQueueActionsWithTimestampAfterAndExclude() = runBlocking {
        // Given
        val timestampAfter = timestamp()
        val exclude = generateArray { timestamp() }
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_QUEUE_ACTIONS)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getQueueActions(timestampAfter, exclude)
        // Then
        assert(response is DataResult.Success)
        assertRequestContainsQueryParam("after", timestampAfter.toString())
        assertRequestContainsQueryParam("exclude", exclude.joinToString(","))
    }

    @Test
    fun postValidateEntitiesData() = runBlocking {
        // Given
        val entitiesHash = listOf(
            EntityHash("entity1", "hash1"),
            EntityHash("entity1", "hash2")
        )
        val mockEngine = createMockResponse(MOCK_RESPONSE_POST_VALIDATE_DATA)
        val service = SynchronizationService(mockEngine, BASE_URL)

        // When
        val response = service.postValidateEntitiesData(entitiesHash)

        // Then
        assert(response is DataResult.Success)
        assertRequestBody(Json.encodeToString(entitiesHash))
        response.fold(
            onSuccess = {
                Assert.assertEquals(2, it.size)
                it.forEach {
                    Assert.assertNotNull(it.entity)
                    Assert.assertNotNull(it.hash)
                    Assert.assertNotNull(it.hash)
                    Assert.assertNotNull(it.hash?.expected)
                    Assert.assertNotNull(it.hash?.obtained)
                    Assert.assertNotNull(it.hash?.matched)
                }
            },
            onFailure = {
                Assert.fail("Error")
            }
        )
    }

    @Test
    fun postValidateHashing() = runBlocking {
        // Given
        val data = mapOf<String,Any>(
            uuid() to uuid(),
            uuid() to uuid()
        )
        val request = ValidateHashingRequest(data, "hash1")
        val mockEngine = createMockResponse(MOCK_RESPONSE_POST_VALIDATE_HASHING)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.postValidateHashing(request)
        // Then
        assert(response is DataResult.Success)
        assertRequestBody(Json.encodeToString(request))
        response.fold(
            onSuccess = {
                Assert.assertNotNull(it.matched)
                Assert.assertNotNull(it.obtained)
                Assert.assertNotNull(it.matched)
            },
            onFailure = {
                Assert.fail("Error")
            }
        )
    }
}