package com.apptank.horus.client.sync.network.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.sync.network.dto.EntityIdHashDTO
import com.apptank.horus.client.sync.network.dto.EntityResponse
import com.apptank.horus.client.sync.network.dto.SyncActionRequest
import com.apptank.horus.client.sync.network.dto.SyncActionResponse
import com.apptank.horus.client.sync.network.dto.SyncDTO
import com.apptank.horus.client.sync.network.dto.ValidateHashingRequest
import com.apptank.horus.client.sync.network.dto.ValidateHashingResponse

interface ISynchronizationService {
    suspend fun getData(timestampAfter: Long? = null): DataResult<List<EntityResponse>>

    suspend fun getDataEntity(
        entity: String,
        afterUpdatedAt: Long? = null,
        ids: List<String> = emptyList()
    ): DataResult<List<EntityResponse>>

    suspend fun postQueueActions(actions: List<SyncActionRequest>): DataResult<Unit>

    suspend fun getQueueActions(
        timestampAfter: Long? = null,
        exclude: List<Long> = emptyList()
    ): DataResult<List<SyncActionResponse>>

    suspend fun postValidateHashing(request: ValidateHashingRequest): DataResult<ValidateHashingResponse>

    suspend fun postValidateEntitiesData(entitiesHash: List<SyncDTO.EntityHash>): DataResult<List<SyncDTO.EntityHashResponse>>

    suspend fun getLastQueueAction(): DataResult<SyncActionResponse>

    suspend fun getEntityHashes(entity: String): DataResult<List<EntityIdHashDTO>>

}