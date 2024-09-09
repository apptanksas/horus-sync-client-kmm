package com.apptank.horus.client.sync.network.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.base.network.BaseService
import com.apptank.horus.client.sync.network.dto.SyncDTO
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation of the [ISynchronizationService] using an [HttpClientEngine] and a base URL.
 *
 * @param engine The HTTP client engine to use for making network requests.
 * @param baseUrl The base URL for the API.
 */
internal class SynchronizationService(
    engine: HttpClientEngine,
    baseUrl: String
) : BaseService(engine, baseUrl), ISynchronizationService {

    /**
     * Retrieves data from the server, optionally after a specified timestamp.
     *
     * @param timestampAfter Optional timestamp to get data updated after this time.
     * @return [DataResult] containing a list of [SyncDTO.Response.Entity] if successful.
     */
    override suspend fun getData(timestampAfter: Long?): DataResult<List<SyncDTO.Response.Entity>> {
        val queryParams = mutableMapOf<String, String>()
        timestampAfter?.let { queryParams["after"] = it.toString() }
        return get("data", queryParams) { it.serialize() }
    }

    /**
     * Retrieves specific entities from the server based on entity name, optional timestamp, and IDs.
     *
     * @param entity The name of the entity to retrieve.
     * @param afterUpdatedAt Optional timestamp to get data updated after this time.
     * @param ids List of IDs to filter the entities.
     * @return [DataResult] containing a list of [SyncDTO.Response.Entity] if successful.
     */
    override suspend fun getDataEntity(
        entity: String,
        afterUpdatedAt: Long?,
        ids: List<String>
    ): DataResult<List<SyncDTO.Response.Entity>> {
        val queryParams = mutableMapOf<String, String>()
        afterUpdatedAt?.let { queryParams["after"] = it.toString() }
        if (ids.isNotEmpty()) {
            queryParams["ids"] = ids.joinToString(",")
        }
        return get("data/${entity.lowercase()}", queryParams) { it.serialize() }
    }

    /**
     * Submits a list of synchronization action requests to the server.
     *
     * @param actions List of [SyncDTO.Request.SyncActionRequest] to submit.
     * @return [DataResult] indicating the success or failure of the operation.
     */
    override suspend fun postQueueActions(actions: List<SyncDTO.Request.SyncActionRequest>): DataResult<Unit> {
        return post("queue/actions", actions) { it.serialize() }
    }

    /**
     * Retrieves synchronization actions from the server, optionally after a specified timestamp and excluding certain IDs.
     *
     * @param timestampAfter Optional timestamp to get actions updated after this time.
     * @param exclude List of IDs to exclude from the results.
     * @return [DataResult] containing a list of [SyncDTO.Response.SyncAction] if successful.
     */
    override suspend fun getQueueActions(
        timestampAfter: Long?,
        exclude: List<Long>
    ): DataResult<List<SyncDTO.Response.SyncAction>> {
        val queryParams = mutableMapOf<String, String>()
        timestampAfter?.let { queryParams["after"] = it.toString() }
        if (exclude.isNotEmpty()) {
            queryParams["exclude"] = exclude.joinToString(",")
        }
        return get("queue/actions", queryParams) { it.serialize() }
    }

    /**
     * Submits a request to validate entity data by comparing hashes.
     *
     * @param entitiesHash List of [SyncDTO.Request.EntityHash] containing entities and their hashes to validate.
     * @return [DataResult] containing a list of [SyncDTO.Response.EntityHash] with validation results if successful.
     */
    override suspend fun postValidateEntitiesData(entitiesHash: List<SyncDTO.Request.EntityHash>): DataResult<List<SyncDTO.Response.EntityHash>> {
        return post("validate/data", entitiesHash) { it.serialize() }
    }

    /**
     * Submits a request to validate hashing data.
     *
     * @param request [SyncDTO.Request.ValidateHashingRequest] containing the data and hash to validate.
     * @return [DataResult] containing [SyncDTO.Response.HashingValidation] if successful.
     */
    override suspend fun postValidateHashing(request: SyncDTO.Request.ValidateHashingRequest): DataResult<SyncDTO.Response.HashingValidation> {
        return post("validate/hashing", request) { it.serialize() }
    }

    /**
     * Retrieves the last synchronization action from the server.
     *
     * @return [DataResult] containing the last [SyncDTO.Response.SyncAction] if successful.
     */
    override suspend fun getLastQueueAction(): DataResult<SyncDTO.Response.SyncAction> {
        return get("queue/actions/last") { it.serialize() }
    }

    /**
     * Retrieves the hash values for a specific entity from the server.
     *
     * @param entity The name of the entity to get hashes for.
     * @return [DataResult] containing a list of [SyncDTO.Response.EntityIdHash] if successful.
     */
    override suspend fun getEntityHashes(entity: String): DataResult<List<SyncDTO.Response.EntityIdHash>> {
        return get("entity/$entity/hashes") { it.serialize() }
    }
}
