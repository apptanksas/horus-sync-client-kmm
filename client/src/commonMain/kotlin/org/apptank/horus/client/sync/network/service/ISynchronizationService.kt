package org.apptank.horus.client.sync.network.service

import okio.Path
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.sync.network.dto.SyncDTO

/**
 * Interface defining the synchronization service operations.
 */
interface ISynchronizationService {


    /**
     * Posts a request to start the synchronization process.
     * This method is typically used to initiate the sync process
     *
     * @param request [SyncDTO.Request.StartSyncRequest] containing the sync ID and optional timestamp.
     */
    suspend fun postStartSync(request: SyncDTO.Request.StartSyncRequest): DataResult<Unit>


    /**
     * Retrieves the status of a synchronization process by its ID.
     *
     * @param syncId The ID of the synchronization process to check.
     * @return [DataResult] containing [SyncDTO.Response.SyncDataStatus] if successful.
     */
    suspend fun getSyncStatus(syncId: String): DataResult<SyncDTO.Response.SyncDataStatus>

    /**
     * Downloads synchronization data from the server.
     *
     * @param url The URL to download the sync data from.
     * @param onProgress A callback function to report download progress as a percentage.
     * @return [DataResult] containing the downloaded data as a path file [String] if successful.
     */
    suspend fun downloadSyncData(url: String, onProgress: (Int) -> Unit): DataResult<Path>

    /**
     * Retrieves data from the server, optionally after a specified timestamp.
     *
     * @param timestampAfter Optional timestamp to get data updated after this time.
     * @return [DataResult] containing a list of [SyncDTO.Response.Entity] if successful.
     */
    suspend fun getData(timestampAfter: Long? = null): DataResult<List<SyncDTO.Response.Entity>>

    /**
     * Retrieves specific entities from the server based on entity name, optional timestamp, and IDs.
     *
     * @param entity The name of the entity to retrieve.
     * @param afterUpdatedAt Optional timestamp to get data updated after this time.
     * @param ids List of IDs to filter the entities.
     * @return [DataResult] containing a list of [SyncDTO.Response.Entity] if successful.
     */
    suspend fun getDataEntity(
        entity: String,
        afterUpdatedAt: Long? = null,
        ids: List<String> = emptyList()
    ): DataResult<List<SyncDTO.Response.Entity>>

    /**
     * Submits a list of synchronization action requests to the server.
     *
     * @param actions List of [SyncDTO.Request.SyncActionRequest] to submit.
     * @return [DataResult] indicating the success or failure of the operation.
     */
    suspend fun postQueueActions(actions: List<SyncDTO.Request.SyncActionRequest>): DataResult<Unit>

    /**
     * Retrieves synchronization actions from the server, optionally after a specified timestamp and excluding certain IDs.
     *
     * @param timestampAfter Optional timestamp to get actions updated after this time.
     * @param exclude List of IDs to exclude from the results.
     * @return [DataResult] containing a list of [SyncDTO.Response.SyncAction] if successful.
     */
    suspend fun getQueueActions(
        timestampAfter: Long? = null,
        exclude: List<Long> = emptyList()
    ): DataResult<List<SyncDTO.Response.SyncAction>>

    /**
     * Submits a request to validate hashing data.
     *
     * @param request [SyncDTO.Request.ValidateHashingRequest] containing the data and hash to validate.
     * @param userId The ID of the user for whom the hashing validation is performed.
     * @return [DataResult] containing [SyncDTO.Response.HashingValidation] if successful.
     */
    suspend fun postValidateHashing(
        request: SyncDTO.Request.ValidateHashingRequest,
        userId: String? = null
    ): DataResult<SyncDTO.Response.HashingValidation>

    /**
     * Submits a request to validate entity data by comparing hashes.
     *
     * @param entitiesHash List of [SyncDTO.Request.EntityHash] containing entities and their hashes to validate.
     * @return [DataResult] containing a list of [SyncDTO.Response.EntityHash] with validation results if successful.
     */
    suspend fun postValidateEntitiesData(entitiesHash: List<SyncDTO.Request.EntityHash>): DataResult<List<SyncDTO.Response.EntityHash>>

    /**
     * Retrieves the last synchronization action from the server.
     *
     * @return [DataResult] containing the last [SyncDTO.Response.SyncAction] if successful.
     */
    suspend fun getLastQueueAction(): DataResult<SyncDTO.Response.SyncAction>

    /**
     * Retrieves the hash values for a specific entity from the server.
     *
     * @param entity The name of the entity to get hashes for.
     * @param userId Optional user ID to filter the hashes.
     * @return [DataResult] containing a list of [SyncDTO.Response.EntityIdHash] if successful.
     */
    suspend fun getEntityHashes(
        entity: String,
        userId: String? = null
    ): DataResult<List<SyncDTO.Response.EntityIdHash>>


    /**
     * Retrieve the data shared for the another users
     *
     * @return [DataResult] containing a list of [SyncDTO.Response.Entity] if successful.
     */
    suspend fun getDataShared(): DataResult<List<SyncDTO.Response.Entity>>

}
