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

class SynchronizationService(
    engine: HttpClientEngine,
    private val baseUrl: String
) : BaseService(engine), ISynchronizationService {
    override suspend fun getData(): DataResult<List<EntityResponse>> {
        return get("$baseUrl/data") { it.serialize() }
    }

    override suspend fun getDataEntity(
        entity: String,
        ids: List<String>
    ): DataResult<List<EntityResponse>> {
        val url = generateUrl(
            baseUrl, "data/${entity.lowercase()}",
            mapOf("ids" to ids.joinToString(","))
        )
        return get(url) { it.serialize() }
    }

    override suspend fun postQueue(actions: List<SyncActionRequest>): DataResult<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getQueueData(
        timestampAfter: Long,
        filter: String
    ): DataResult<List<SyncActionResponse>> {
        TODO("Not yet implemented")
    }

    override suspend fun postValidateHashing(request: ValidateHashingRequest): DataResult<ValidateHashingResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun postValidateData(entitiesHash: List<EntityHash>): DataResult<List<EntityHashResponse>> {
        TODO("Not yet implemented")
    }

    override suspend fun getLastAction(): DataResult<SyncActionResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun getEntityHashes(entity: String): DataResult<List<EntityIdHashDTO>> {
        TODO("Not yet implemented")
    }

}