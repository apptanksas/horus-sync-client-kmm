package com.apptank.horus.client.sync.network.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.base.network.BaseService
import com.apptank.horus.client.sync.network.dto.SyncDTO
import io.ktor.client.engine.HttpClientEngine

internal class SynchronizationService(
    engine: HttpClientEngine,
    baseUrl: String
) : BaseService(engine, baseUrl), ISynchronizationService {
    override suspend fun getData(timestampAfter: Long?): DataResult<List<SyncDTO.Response.Entity>> {

        val queryParams = mutableMapOf<String, String>()
        timestampAfter?.let { queryParams["after"] = it.toString() }

        return get("data", queryParams) { it.serialize() }
    }

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

    override suspend fun postQueueActions(actions: List<SyncDTO.Request.SyncActionRequest>): DataResult<Unit> {
        return post("queue/actions", actions) { it.serialize() }
    }

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

    override suspend fun postValidateEntitiesData(entitiesHash: List<SyncDTO.EntityHash>): DataResult<List<SyncDTO.Response.EntityHash>> {
        return post("validate/data", entitiesHash) { it.serialize() }
    }

    override suspend fun postValidateHashing(request: SyncDTO.Request.ValidateHashingRequest): DataResult<SyncDTO.Response.HashingValidation> {
        return post("validate/hashing", request) { it.serialize() }
    }

    override suspend fun getLastQueueAction(): DataResult<SyncDTO.Response.SyncAction> {
        return get("queue/actions/last") { it.serialize() }
    }

    override suspend fun getEntityHashes(entity: String): DataResult<List<SyncDTO.Response.EntityIdHash>> {
        return get("entity/$entity/hashes") { it.serialize() }
    }

}