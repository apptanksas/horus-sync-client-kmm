package com.apptank.horus.client.sync.network.service

import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.sync.network.dto.EntityHash
import com.apptank.horus.client.sync.network.dto.EntityHashResponse
import com.apptank.horus.client.sync.network.dto.EntityIdHashDTO
import com.apptank.horus.client.sync.network.dto.EntityResponse
import com.apptank.horus.client.sync.network.dto.SyncActionRequest
import com.apptank.horus.client.sync.network.dto.SyncActionResponse
import com.apptank.horus.client.sync.network.dto.ValidateHashingRequest
import com.apptank.horus.client.sync.network.dto.ValidateHashingResponse

interface ISynchronizationService {
    suspend fun getData(): DataResult<List<EntityResponse>>

    suspend fun getDataEntity(
        entity: String,
        ids: List<String> = emptyList()
    ): DataResult<List<EntityResponse>>

    suspend fun postQueue(actions: List<SyncActionRequest>): DataResult<Unit>

    suspend fun getQueueData(
        timestampAfter: Long,
        filter: String
    ): DataResult<List<SyncActionResponse>>

    suspend fun postValidateHashing(request: ValidateHashingRequest): DataResult<ValidateHashingResponse>

    suspend fun postValidateData(entitiesHash: List<EntityHash>): DataResult<List<EntityHashResponse>>

    suspend fun getLastAction(): DataResult<SyncActionResponse>

    suspend fun getEntityHashes(entity: String): DataResult<List<EntityIdHashDTO>>

}