package com.apptank.horus.client.sync.network.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.sync.network.dto.SyncDTO

interface ISynchronizationService {

    suspend fun getData(timestampAfter: Long? = null): DataResult<List<SyncDTO.Response.Entity>>

    suspend fun getDataEntity(
        entity: String,
        afterUpdatedAt: Long? = null,
        ids: List<String> = emptyList()
    ): DataResult<List<SyncDTO.Response.Entity>>

    suspend fun postQueueActions(actions: List<SyncDTO.Request.SyncActionRequest>): DataResult<Unit>

    suspend fun getQueueActions(
        timestampAfter: Long? = null,
        exclude: List<Long> = emptyList()
    ): DataResult<List<SyncDTO.Response.SyncAction>>

    suspend fun postValidateHashing(request: SyncDTO.Request.ValidateHashingRequest): DataResult<SyncDTO.Response.HashingValidation>

    suspend fun postValidateEntitiesData(entitiesHash: List<SyncDTO.Request.EntityHash>): DataResult<List<SyncDTO.Response.EntityHash>>

    suspend fun getLastQueueAction(): DataResult<SyncDTO.Response.SyncAction>

    suspend fun getEntityHashes(entity: String): DataResult<List<SyncDTO.Response.EntityIdHash>>

}