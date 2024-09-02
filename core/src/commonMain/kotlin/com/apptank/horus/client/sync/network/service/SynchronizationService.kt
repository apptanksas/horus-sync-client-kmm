package com.apptank.horus.client.sync.network.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.base.network.BaseService
import com.apptank.horus.client.sync.network.dto.EntityHash
import com.apptank.horus.client.sync.network.dto.EntityHashResponse
import com.apptank.horus.client.sync.network.dto.EntityIdHashDTO
import com.apptank.horus.client.sync.network.dto.EntityResponse
import com.apptank.horus.client.sync.network.dto.SyncActionRequest
import com.apptank.horus.client.sync.network.dto.SyncActionResponse
import com.apptank.horus.client.sync.network.dto.ValidateHashingRequest
import com.apptank.horus.client.sync.network.dto.ValidateHashingResponse
import io.ktor.client.engine.HttpClientEngine

internal class SynchronizationService(
    engine: HttpClientEngine,
    baseUrl: String
) : BaseService(engine, baseUrl), ISynchronizationService {
    override suspend fun getData(timestampAfter: Long?): DataResult<List<EntityResponse>> {

        val queryParams = mutableMapOf<String, String>()
        timestampAfter?.let { queryParams["after"] = it.toString() }

        return get("data", queryParams) { it.serialize() }
    }

    override suspend fun getDataEntity(
        entity: String,
        afterUpdatedAt: Long?,
        ids: List<String>
    ): DataResult<List<EntityResponse>> {

        val queryParams = mutableMapOf<String, String>()

        afterUpdatedAt?.let { queryParams["after"] = it.toString() }

        if (ids.isNotEmpty()) {
            queryParams["ids"] = ids.joinToString(",")
        }

        return get("data/${entity.lowercase()}", queryParams) { it.serialize() }
    }

    override suspend fun postQueueActions(actions: List<SyncActionRequest>): DataResult<Unit> {
        return post("queue/actions", actions) { it.serialize() }
    }

    override suspend fun getQueueActions(
        timestampAfter: Long?,
        exclude: List<Long>
    ): DataResult<List<SyncActionResponse>> {

        val queryParams = mutableMapOf<String, String>()

        timestampAfter?.let { queryParams["after"] = it.toString() }

        if (exclude.isNotEmpty()) {
            queryParams["exclude"] = exclude.joinToString(",")
        }

        return get("queue/actions", queryParams) { it.serialize() }
    }

    override suspend fun postValidateEntitiesData(entitiesHash: List<EntityHash>): DataResult<List<EntityHashResponse>> {
        return post("validate/data", entitiesHash) { it.serialize() }
    }

    override suspend fun postValidateHashing(request: ValidateHashingRequest): DataResult<ValidateHashingResponse> {
        return post("validate/hashing", request) { it.serialize() }
    }

    override suspend fun getLastQueueAction(): DataResult<SyncActionResponse> {
        return get("queue/actions/last") { it.serialize() }
    }

    override suspend fun getEntityHashes(entity: String): DataResult<List<EntityIdHashDTO>> {
        return get("entity/$entity/hashes") { it.serialize() }
    }

}