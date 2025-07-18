package org.apptank.horus.client.sync.network.service

import org.apptank.horus.client.MOCK_RESPONSE_GET_DATA
import org.apptank.horus.client.MOCK_RESPONSE_GET_DATA_ENTITY
import org.apptank.horus.client.MOCK_RESPONSE_GET_ENTITY_HASHES
import org.apptank.horus.client.MOCK_RESPONSE_GET_LAST_QUEUE_ACTION
import org.apptank.horus.client.MOCK_RESPONSE_GET_QUEUE_ACTIONS
import org.apptank.horus.client.MOCK_RESPONSE_INTERNAL_SERVER_ERROR
import org.apptank.horus.client.MOCK_RESPONSE_POST_VALIDATE_DATA
import org.apptank.horus.client.MOCK_RESPONSE_POST_VALIDATE_HASHING
import org.apptank.horus.client.ServiceTest
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.base.fold
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.sync.network.dto.SyncDTO
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apptank.horus.client.MOCK_RESPONSE_GET_DATA_SHARED
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
        assertRequestMissingQueryParam("after")
    }

    @Test
    fun getDataAfterIsSuccess() = runBlocking {
        // Given
        val timestampAfter = timestamp()
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_DATA)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getData(timestampAfter)
        // Then
        assert(response is DataResult.Success)
        assertRequestContainsQueryParam("after", timestampAfter.toString())
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
        val response = service.getDataEntity("products")
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
        assertRequestMissingQueryParam("after")
        assertRequestMissingQueryParam("ids")
    }

    @Test
    fun getDataEntityWithIds() = runBlocking {
        // Given
        val ids = generateRandomArray { uuid() }.map { it }
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_DATA_ENTITY)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getDataEntity("products", ids = ids)
        // Then
        assert(response is DataResult.Success)
        assertRequestContainsQueryParam("ids", ids.joinToString(","))
        assertRequestMissingQueryParam("after")
    }

    @Test
    fun getDataEntityAfterIsSuccess() = runBlocking {
        // Given
        val timestampAfter = timestamp()
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_DATA_ENTITY)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getDataEntity("products", timestampAfter)
        // Then
        assert(response is DataResult.Success)
        assertRequestContainsQueryParam("after", timestampAfter.toString())
        assertRequestMissingQueryParam("ids")
    }

    @Test
    fun postQueueActions() = runBlocking {

        // Given
        val actions = generateRandomArray {
            SyncDTO.Request.SyncActionRequest(
                SyncControl.ActionType.INSERT.name, "products", mapOf(
                    "id" to uuid(),
                    "name" to "Product  ${uuid()}"
                ), timestamp() - (it * 60)
            )
        } + generateRandomArray {
            SyncDTO.Request.SyncActionRequest(
                SyncControl.ActionType.UPDATE.name, "products", mapOf(
                    "id" to uuid(),
                    "attributes" to mapOf(
                        "name" to "Attribute ${uuid()}",
                        "boolean" to true,
                        "int" to 1,
                        "double" to 1.0,
                        "float" to 1.0f,
                    )
                ), timestamp() + (it * 60)
            )
        }
        val mockEngine = createMockResponse(status = HttpStatusCode.Created)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.postQueueActions(actions)
        // Then
        assert(response is DataResult.Success)
        assertRequestBody(Json.encodeToString(actions.sortedBy { it.actionedAt }))

        // Validate request body
        Json.decodeFromString<List<SyncDTO.Response.SyncAction>>(Json.encodeToString(actions))
            .filter {
                it.action == SyncControl.ActionType.UPDATE.name
            }.forEach {
                Assert.assertTrue(
                    "Attributes is type " + it.data!!["attributes"]!!::class.simpleName,
                    it.data?.get("attributes") is LinkedHashMap<*, *>
                )
            }
    }

    @Test
    fun postQueueActionsChunked() = runBlocking {

        // Given
        val actions = generateArray (1000) {
            SyncDTO.Request.SyncActionRequest(
                SyncControl.ActionType.INSERT.name, "products", mapOf(
                    "id" to uuid(),
                    "name" to "Product  ${uuid()}"
                ), timestamp() - (it * 60)
            )
        } +  generateArray (1000) {
            SyncDTO.Request.SyncActionRequest(
                SyncControl.ActionType.UPDATE.name, "products", mapOf(
                    "id" to uuid(),
                    "attributes" to mapOf(
                        "name" to "Attribute ${uuid()}",
                        "boolean" to true,
                        "int" to 1,
                        "double" to 1.0,
                        "float" to 1.0f,
                    )
                ), timestamp() + (it * 60)
            )
        }
        val mockEngine = createMockResponse(status = HttpStatusCode.Created)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.postQueueActions(actions)
        // Then
        assert(response is DataResult.Success)
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
        val exclude = generateRandomArray { timestamp() + it }
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
        val exclude = generateRandomArray { timestamp() + it }
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
    fun getQueueActionsWithTimestampAfterAndExcludeUniques() = runBlocking {
        // Given
        val timestampAfter = timestamp()
        val excludeTimestamp = timestamp()
        val exclude = generateArray(10) { excludeTimestamp }
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_QUEUE_ACTIONS)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getQueueActions(timestampAfter, exclude)
        // Then
        assert(response is DataResult.Success)
        assertRequestContainsQueryParam("after", timestampAfter.toString())
        assertRequestContainsQueryParam("exclude", excludeTimestamp.toString())
    }


    @Test
    fun postValidateEntitiesData() = runBlocking {
        // Given
        val entitiesHash = listOf(
            SyncDTO.Request.EntityHash("entity1", "hash1"),
            SyncDTO.Request.EntityHash("entity1", "hash2")
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
                    Assert.assertNotNull(it.hashingValidation)
                    Assert.assertNotNull(it.hashingValidation)
                    Assert.assertNotNull(it.hashingValidation?.expected)
                    Assert.assertNotNull(it.hashingValidation?.obtained)
                    Assert.assertNotNull(it.hashingValidation?.matched)
                }
            },
            onFailure = {
                Assert.fail("Error")
            }
        )
    }

    @Test
    fun postValidateEntitiesDataIsFailure() = runBlocking {
        // Given
        val entitiesHash = listOf(
            SyncDTO.Request.EntityHash("entity1", "hash1"),
            SyncDTO.Request.EntityHash("entity1", "hash2")
        )
        val mockEngine = createMockResponse(
            MOCK_RESPONSE_INTERNAL_SERVER_ERROR,
            status = HttpStatusCode.InternalServerError
        )
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.postValidateEntitiesData(entitiesHash)
        // Then
        assert(response is DataResult.Failure)
    }

    @Test
    fun postValidateHashing() = runBlocking {
        // Given
        val data = mapOf<String, Any>(
            uuid() to uuid(),
            uuid() to uuid()
        )
        val request = SyncDTO.Request.ValidateHashingRequest(data, "hash1")
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

    @Test
    fun getLastQueueAction() = runBlocking {
        // Given
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_LAST_QUEUE_ACTION)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getLastQueueAction()
        // Then
        assert(response is DataResult.Success)
        response.fold(
            onSuccess = {
                Assert.assertNotNull(it.action)
                Assert.assertNotNull(it.entity)
                Assert.assertNotNull(it.data)
                Assert.assertFalse(it.data?.isEmpty() ?: true)
                Assert.assertNotNull(it.actionedAt)
                Assert.assertNotNull(it.syncedAt)
            },
            onFailure = {
                Assert.fail("Error")
            }
        )
    }

    @Test
    fun getLastQueueActionIsEmptyWithObjectEmpty() = runBlocking {
        // Given
        val mockEngine = createMockResponse("{}")
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getLastQueueAction()
        // Then
        assert(response is DataResult.Success)
    }

    @Test
    fun getLastQueueActionIsEmptyWithArrayEmpty() = runBlocking {
        // Given
        val mockEngine = createMockResponse("[]")
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getLastQueueAction()
        // Then
        assert(response is DataResult.Success)
    }

    @Test
    fun getEntityHashes() = runBlocking {
        // Given
        val entity = "entity123"
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_ENTITY_HASHES)
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getEntityHashes(entity)
        // Then
        assert(response is DataResult.Success)
        response.fold(
            onSuccess = {
                Assert.assertEquals(1, it.size)
                it.forEach {
                    Assert.assertNotNull(it.id)
                    Assert.assertNotNull(it.hash)
                }
            },
            onFailure = {
                Assert.fail("Error")
            })
    }

    @Test
    fun getEntityHashesIsEmptyWithArrayEmpty() = runBlocking {
        // Given
        val entity = "entity123"
        val mockEngine = createMockResponse("[]")
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getEntityHashes(entity)
        // Then
        assert(response is DataResult.Success)
        if (response is DataResult.Success) {
            Assert.assertTrue(response.data.isEmpty())
        }
    }

    @Test
    fun getEntityHashesIsEmptyWithObjectEmpty() = runBlocking {
        // Given
        val entity = "entity123"
        val mockEngine = createMockResponse("{}")
        val service = SynchronizationService(mockEngine, BASE_URL)
        // When
        val response = service.getEntityHashes(entity)
        // Then
        assert(response is DataResult.Success)
        if (response is DataResult.Success) {
            Assert.assertTrue(response.data.isEmpty())
        }
    }

    @Test
    fun getDataSharedIsSuccess() = runBlocking {
        // Given
        val mockEngine = createMockResponse(MOCK_RESPONSE_GET_DATA_SHARED)
        val service = SynchronizationService(mockEngine, BASE_URL)

        // When
        val response = service.getDataShared()

        // Then
        assert(response is DataResult.Success)
        if (response is DataResult.Success) {
            Assert.assertTrue(response.data.isNotEmpty())
        }
    }

    @Test
    fun validateWithCustomHeaders() = runBlocking {
        // Given
        val entitiesHash = listOf(
            SyncDTO.Request.EntityHash("entity1", "hash1"),
            SyncDTO.Request.EntityHash("entity1", "hash2")
        )
        val mockEngine = createMockResponse(MOCK_RESPONSE_POST_VALIDATE_DATA)
        val service = SynchronizationService(mockEngine, BASE_URL, customHeaders = mapOf("X-Custom-Header" to "CustomValue"))

        // When
        val response = service.postValidateEntitiesData(entitiesHash)

        // Then
        assert(response is DataResult.Success)
        assertRequestBody(Json.encodeToString(entitiesHash))
        assertRequestHeader("X-Custom-Header", "CustomValue")
    }
}