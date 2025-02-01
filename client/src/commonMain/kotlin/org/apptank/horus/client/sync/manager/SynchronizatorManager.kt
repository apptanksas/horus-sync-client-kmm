package org.apptank.horus.client.sync.manager

import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.data.InternalModel
import org.apptank.horus.client.data.toDTORequest
import org.apptank.horus.client.database.struct.DatabaseOperation
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.database.struct.toDeleteRecord
import org.apptank.horus.client.database.struct.toInsertRecord
import org.apptank.horus.client.database.struct.toRecordsInsert
import org.apptank.horus.client.database.struct.toUpdateRecord
import org.apptank.horus.client.exception.UserNotAuthenticatedException
import org.apptank.horus.client.extensions.evaluate
import org.apptank.horus.client.extensions.isTrue
import org.apptank.horus.client.extensions.log
import org.apptank.horus.client.hashing.AttributeHasher
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.extensions.warn
import org.apptank.horus.client.sync.network.dto.toDomain
import org.apptank.horus.client.sync.network.dto.toEntityData
import org.apptank.horus.client.sync.network.dto.toInternalModel
import org.apptank.horus.client.sync.network.service.ISynchronizationService

/**
 * Manages data synchronization between local storage and a remote server.
 *
 * This class handles the process of synchronizing data with a remote server, including network validation,
 * checking for pending actions, validating and restoring corrupted data, and updating the synchronization status.
 *
 * @param netWorkValidator An instance of `INetworkValidator` to monitor network availability.
 * @param syncControlDatabaseHelper An instance of `ISyncControlDatabaseHelper` for managing sync control data.
 * @param operationDatabaseHelper An instance of `IOperationDatabaseHelper` for handling database operations.
 * @param synchronizationService An instance of `ISynchronizationService` for interacting with the remote server.
 */
internal class SynchronizatorManager(
    private val netWorkValidator: INetworkValidator,
    private val syncControlDatabaseHelper: ISyncControlDatabaseHelper,
    private val operationDatabaseHelper: IOperationDatabaseHelper,
    private val synchronizationService: ISynchronizationService
) {

    /**
     * Represents the status of the synchronization process.
     */
    enum class SynchronizationStatus {
        SUCCESS,
        IN_PROGRESS,
        FAILED,
        IDLE
    }

    /**
     * Starts the synchronization process and invokes the provided callback with the status and completion flag.
     *
     * This method performs several stages of synchronization, including checking network availability,
     * validating data to sync, restoring corrupted data, and updating the synchronization status.
     *
     * @param onStatus A callback function to receive the synchronization status and a completion flag.
     */
    suspend fun start(onStatus: (SynchronizationStatus, isCompleted: Boolean) -> Unit) {

        if (HorusAuthentication.isNotUserAuthenticated()) {
            warn("[SynchronizatorManager] User is not authenticated")
            return onStatus(SynchronizationStatus.IDLE, true)
        }

        // Validate if there is network available
        if (!netWorkValidator.isNetworkAvailable()) {
            log("[SynchronizatorManager] No network available")
            return onStatus(SynchronizationStatus.IDLE, true)
        }

        // Validate if there are pending actions to sync with the server
        if (syncControlDatabaseHelper.getPendingActions().isNotEmpty()) {
            log("[SynchronizatorManager] There are pending actions")
            return onStatus(SynchronizationStatus.IDLE, true)
        }

        onStatus(SynchronizationStatus.IN_PROGRESS, false)

        // Stage 1: Validate if there are new data to sync with the server
        val validateIsExistsDataToSync =
            existsDataToSync() ?: return onStatus(SynchronizationStatus.FAILED, true)

        if (validateIsExistsDataToSync.isTrue()) {
            log("[SynchronizatorManager] There are new data to sync with the server")

            return onStatus(
                synchronizeData().evaluate(
                    SynchronizationStatus.SUCCESS,
                    SynchronizationStatus.FAILED
                ), true
            )
        }

        // Stage 2: Validate entity hashes
        val entitiesHashes = getEntityHashes()
        val entitiesHashesValidated = validateEntityHashes(entitiesHashes)

        // EntityName -> List of corrupted ids
        val corruptedEntities = mutableMapOf<String, List<String>>()
        val missingEntities = mutableMapOf<String, List<String>>()

        entitiesHashesValidated.forEach {
            val entity = it.first
            val isHashCorrect = it.second
            // First -> Entity name, Second -> Validation result
            log("[SynchronizatorManager] Entity: $entity - IsHashCorrect: $isHashCorrect")

            if (!isHashCorrect) {
                val (corrupted, missing) = validateEntityData(entity)
                corruptedEntities[it.first] = corrupted
                missingEntities[it.first] = missing
            }
        }

        val integrityDataRestoredResult = mutableListOf<Boolean>()

        // Stage 3: Restore corrupted data
        corruptedEntities.forEach { (entity, ids) ->
            if (ids.isNotEmpty()) {
                val resultRestoreCorruptedData = restoreCorruptedData(entity, ids)
                if (resultRestoreCorruptedData) {
                    log("[SynchronizatorManager][entity:$entity] Corrupted data restored successfully")
                } else {
                    log("[SynchronizatorManager][entity:$entity] Error restoring corrupted data")
                }
                integrityDataRestoredResult.add(resultRestoreCorruptedData)
            } else {
                log("[SynchronizatorManager][entity:$entity] No corrupted data to restore")
            }
        }

        // Stage 4: Sync missing data
        missingEntities.map { (entity, ids) ->
            if (ids.isNotEmpty()) {
                val resultSyncMissingData = syncEntitiesMissingData(entity, ids)
                if (resultSyncMissingData) {
                    log("[SynchronizatorManager][entity:$entity] Missing data synced successfully")
                } else {
                    log("[SynchronizatorManager][entity:$entity] Error restoring missing data")
                }
                integrityDataRestoredResult.add(resultSyncMissingData)
            } else {
                log("[SynchronizatorManager][entity:$entity] No missing data to restore")
            }
        }

        return onStatus(
            integrityDataRestoredResult.all { it }.evaluate(
                SynchronizationStatus.SUCCESS,
                SynchronizationStatus.FAILED
            ), true
        )
    }

    /**
     * Checks if there is data to synchronize with the server.
     *
     * This method verifies if there are any actions pending synchronization since the last checkpoint.
     *
     * @return `true` if there is data to sync, `false` otherwise, or `null` if an error occurred.
     */
    private suspend fun existsDataToSync(): Boolean? {

        val checkpointTimestamp = syncControlDatabaseHelper.getLastDatetimeCheckpoint()
        val lastActions =
            syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp)

        val resultActions = synchronizationService.getQueueActions(
            checkpointTimestamp,
            lastActions.map { it.getActionedAtTimestamp() })

        when (resultActions) {
            is DataResult.Success -> {
                // If there are actions, it means that there is data to sync
                return resultActions.data.isNotEmpty()
            }

            is DataResult.Failure -> {
                resultActions.exception.printStackTrace()
                log("[SynchronizatorManager] Error getting queue data")
            }

            is DataResult.NotAuthorized -> {
                log("[SynchronizatorManager] Not authorized")
            }
        }

        return null
    }

    /**
     * Retrieves the hash values of all entities from the local database.
     *
     * This method gathers the hashes for each entity to validate data integrity.
     *
     * @return A list of entity hashes.
     */
    private fun getEntityHashes(): List<Horus.EntityHash> {

        val output = mutableListOf<Horus.EntityHash>()

        syncControlDatabaseHelper.getWritableEntityNames().forEach { entity ->
            val hashes = mutableListOf<String>()
            // Create a query to get the id and sync_hash of the entity
            val queryBuilder = SimpleQueryBuilder(entity)
            queryBuilder.select(Horus.Attribute.HASH).orderBy("id")

            // Execute the query and get the hashes
            operationDatabaseHelper.queryRecords(queryBuilder).forEach { record ->
                hashes.add(record[Horus.Attribute.HASH] as String)
            }

            if (hashes.isNotEmpty()) {
                output.add(
                    Horus.EntityHash(
                        entity,
                        AttributeHasher.generateHashFromList(hashes)
                    )
                )
            }
        }

        return output
    }

    /**
     * Validates entity hashes against remote data.
     *
     * This method compares local entity hashes with those stored on the remote server.
     *
     * @param entitiesHashes A list of entity hashes to validate.
     * @return A list of pairs containing entity names and validation results.
     */
    private suspend fun validateEntityHashes(entitiesHashes: List<Horus.EntityHash>): List<Pair<String, Boolean>> {
        return validateRemoteEntitiesData(entitiesHashes).map { Pair(it.entity, it.isHashMatched) }
    }

    /**
     * Checks the integrity of entity data.
     *
     * This method compares local data with remote hashes to find discrepancies.
     *
     * @param entity The name of the entity to check.
     * @return A pair of lists containing IDs with corrupted data and IDs with missing data. [Corrupted, Missing]
     */
    private suspend fun validateEntityData(entity: String): Pair<List<String>, List<String>> {
        val remoteHashes = getRemoteEntitiesHashes(entity)
        return compareEntityHashesWithLocalData(entity, remoteHashes)
    }

    /**
     * Compares local entity hashes with remote hashes to identify discrepancies and missing data.
     *
     * @param entity The name of the entity to check.
     * @param remoteHashes A list of remote entity hashes.
     * @return A list of IDs with corrupted data and a list of IDs with missing data. [Corrupted, Missing]
     */
    private fun compareEntityHashesWithLocalData(
        entity: String,
        remoteHashes: List<InternalModel.EntityIdHash>
    ): Pair<List<String>, List<String>> {

        val corruptedIds = mutableListOf<String>()
        val missingIds = mutableListOf<String>()

        val localIdsHashes = operationDatabaseHelper.queryRecords(
            SimpleQueryBuilder(entity).select(Horus.Attribute.ID, Horus.Attribute.HASH)
        ).associate { it[Horus.Attribute.ID].toString() to it[Horus.Attribute.HASH].toString() }

        remoteHashes.forEach {
            val localHash = localIdsHashes[it.id]
            val entityExists = localHash != null

            when {
                entityExists && localHash != it.hash -> {
                    log("[SynchronizatorManager:compareEntityHashesWithLocalData] Problem detected integrity -> Entity: $entity - Id: ${it.id} - Local: $localHash - Remote: ${it.hash}")
                    corruptedIds.add(it.id)
                }

                entityExists.not() -> {
                    log("[SynchronizatorManager:compareEntityHashesWithLocalData] Problem detected missing -> Entity: $entity - Id: ${it.id} - Remote: ${it.hash}")
                    missingIds.add(it.id)
                }
            }
        }

        return Pair(corruptedIds, missingIds)
    }

    /**
     * Restores corrupted data for a specific entity.
     *
     * This method fetches data from the remote server and replaces corrupted local records.
     *
     * @param entity The name of the entity to restore.
     * @param ids A list of IDs with corrupted data.
     * @return `true` if the restoration was successful, `false` otherwise.
     */
    private suspend fun restoreCorruptedData(entity: String, ids: List<String>): Boolean {

        when (val dataEntitiesResponse = synchronizationService.getDataEntity(entity, ids = ids)) {

            is DataResult.Success -> {
                // Delete ids with corrupted data
                val result = operationDatabaseHelper.deleteRecords(
                    entity,
                    ids.map { SQL.WhereCondition(SQL.ColumnValue(Horus.Attribute.ID, it)) },
                    SQL.LogicOperator.OR
                )

                if (!result.isSuccess) {
                    log("[SynchronizatorManager:restoreCorruptedData] Error deleting corrupted data")
                    return false
                }

                return operationDatabaseHelper.insertWithTransaction(
                    dataEntitiesResponse.data.map { it.toEntityData() }
                        .flatMap { it.toRecordsInsert() })
            }

            is DataResult.Failure -> {
                dataEntitiesResponse.exception.printStackTrace()
                log("[SynchronizatorManager:restoreCorruptedData] Error restoring corrupted data")
            }

            is DataResult.NotAuthorized -> {
                log("[SynchronizatorManager:restoreCorruptedData] Not authorized")
            }
        }

        return false
    }

    /**
     * Restores missing data for a specific entity.
     *
     * This method fetches data from the remote server and inserts missing local records.
     *
     * @param entity The name of the entity to restore.
     * @param ids A list of IDs with missing data.
     * @return `true` if the restoration was successful, `false` otherwise.
     */
    private suspend fun syncEntitiesMissingData(entity: String, ids: List<String>): Boolean {
        when (val dataEntitiesResponse = synchronizationService.getDataEntity(entity, ids = ids)) {
            is DataResult.Success -> {
                return operationDatabaseHelper.insertWithTransaction(
                    dataEntitiesResponse.data.map { it.toEntityData() }
                        .flatMap { it.toRecordsInsert() })
            }

            is DataResult.Failure -> {
                dataEntitiesResponse.exception.printStackTrace()
                log("[SynchronizatorManager:syncEntitiesMissingData] Error restoring missing data")
            }

            is DataResult.NotAuthorized -> {
                log("[SynchronizatorManager:syncEntitiesMissingData] Not authorized")
            }
        }
        return false
    }

    /**
     * Synchronizes local data with the remote server.
     *
     * This method retrieves new actions since the last checkpoint and performs the necessary database operations.
     *
     * @return `true` if synchronization was successful, `false` otherwise.
     */
    private suspend fun synchronizeData(): Boolean {

        val checkpointDatetime = syncControlDatabaseHelper.getLastDatetimeCheckpoint()

        if (checkpointDatetime == 0L) {
            log("[SynchronizatorManager] No checkpoint datetime")
            return true
        }

        log("[SynchronizatorManager] Synchronizing data from checkpoint datetime: $checkpointDatetime")

        val actions = synchronizationService.getQueueActions(checkpointDatetime)

        when (actions) {
            is DataResult.Success -> {

                val newActions =
                    filterOwnActions(actions.data.map { it.toDomain() }, checkpointDatetime)

                val (actionsInsert, updateActions, deleteActions) = organizeActions(newActions)

                val operations = mapToInsertOperation(actionsInsert) +
                        mapToUpdateOperation(updateActions) +
                        mapToDeleteOperation(deleteActions)

                val result = operationDatabaseHelper.executeOperations(operations)

                val syncControlStatus = if (result) {
                    log("[SynchronizatorManager:synchronizeData] Data synchronized successfully")
                    SyncControl.Status.COMPLETED
                } else {
                    log("[SynchronizatorManager:synchronizeData] Error synchronizing data")
                    SyncControl.Status.FAILED
                }
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    syncControlStatus
                )

                return result
            }

            is DataResult.Failure -> {
                actions.exception.printStackTrace()
                log("[SynchronizatorManager] Error getting queue data")
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.FAILED
                )
                return false
            }

            is DataResult.NotAuthorized -> {
                log("[SynchronizatorManager] Not authorized")
                return false
            }
        }
    }

    /**
     * Filters out actions that are not present in the local database.
     *
     * @param actions A list of synchronization actions.
     * @param checkpointTimestamp The timestamp of the last checkpoint.
     * @return A filtered list of actions to be processed.
     */
    private fun filterOwnActions(
        actions: List<SyncControl.Action>,
        checkpointTimestamp: Long
    ): List<SyncControl.Action> {

        val ownActions =
            syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp)

        // Filter the actions that are not in the local database
        return actions.filterNot { action ->
            ownActions.find { it.getActionedAtTimestamp() == action.getActionedAtTimestamp() } != null
        }
    }

    /**
     * Maps actions to insert operations.
     *
     * @param actions A list of actions to insert.
     * @return A list of insert operations.
     */
    private fun mapToInsertOperation(actions: List<SyncControl.Action>): List<DatabaseOperation.InsertRecord> {
        return actions.map { it.toInsertRecord(getUserId()) }
    }

    /**
     * Maps actions to update operations.
     *
     * @param actions A list of actions to update.
     * @return A list of update operations.
     */
    private fun mapToUpdateOperation(actions: List<SyncControl.Action>): List<DatabaseOperation.UpdateRecord> {

        val actionsUpdate = actions.mapNotNull {
            getEntityById(it.entity, it.getEntityId())?.let { entity ->
                it.toUpdateRecord(entity)
            } ?: run {
                log("[SynchronizatorManager] Error updating data. [${it.data}]")
                null
            }
        }

        return actionsUpdate
    }

    /**
     * Maps actions to delete operations.
     *
     * @param actions A list of actions to delete.
     * @return A list of delete operations.
     */
    private fun mapToDeleteOperation(actions: List<SyncControl.Action>): List<DatabaseOperation.DeleteRecord> {
        return actions.map { it.toDeleteRecord() }
    }

    /**
     * Retrieves an entity by its ID.
     *
     * @param entity The name of the entity.
     * @param id The ID of the entity.
     * @return The entity if found, `null` otherwise.
     */
    private fun getEntityById(entity: String, id: String): Horus.Entity? {

        val queryBuilder = SimpleQueryBuilder(entity).apply {
            where(
                SQL.WhereCondition(
                    SQL.ColumnValue(Horus.Attribute.ID, id)
                )
            )
        }

        return operationDatabaseHelper.queryRecords(queryBuilder).map {
            Horus.Entity(
                entity,
                it.map { Horus.Attribute(it.key, it.value) }
            )
        }.firstOrNull()
    }

    /**
     * Retrieves the authenticated user ID.
     *
     * @return The authenticated user ID.
     * @throws UserNotAuthenticatedException If the user is not authenticated.
     */
    private fun getUserId(): String {
        return HorusAuthentication.getUserAuthenticatedId() ?: throw UserNotAuthenticatedException()
    }

    /**
     * Retrieves remote entity hashes.
     *
     * @param entity The name of the entity.
     * @return A list of remote entity hashes.
     */
    private suspend fun getRemoteEntitiesHashes(entity: String): List<InternalModel.EntityIdHash> {
        when (val result = synchronizationService.getEntityHashes(entity)) {
            is DataResult.Success -> {
                return result.data.map { it.toInternalModel() }
            }

            is DataResult.Failure -> {
                result.exception.printStackTrace()
                log("[SynchronizatorManager:getEntitiesHashes] Error validating entity data")
            }

            is DataResult.NotAuthorized -> {
                log("[SynchronizatorManager:getEntitiesHashes] Not authorized")
            }
        }

        return emptyList()
    }

    /**
     * Validates entity data against remote data.
     *
     * @param entitiesHashes A list of entity hashes to validate.
     * @return A list of entity hash validation results.
     */
    private suspend fun validateRemoteEntitiesData(entitiesHashes: List<Horus.EntityHash>): List<InternalModel.EntityHashValidation> {
        when (val result =
            synchronizationService.postValidateEntitiesData(entitiesHashes.toDTORequest())) {
            is DataResult.Success -> {
                return result.data.map { it.toInternalModel() }
            }

            is DataResult.Failure -> {
                result.exception.printStackTrace()
                log("[SynchronizatorManager:getRemoteValidateEntitiesData] Error validating entity data")
            }

            is DataResult.NotAuthorized -> {
                log("[SynchronizatorManager:getRemoteValidateEntitiesData] Not authorized")
            }
        }

        return emptyList()
    }

    /**
     * Organizes synchronization actions into insert, update, and delete operations.
     *
     * @param syncActions A list of synchronization actions.
     * @return A triple containing lists of insert, update, and delete actions.
     */
    private fun organizeActions(syncActions: List<SyncControl.Action>): Triple<List<SyncControl.Action>, List<SyncControl.Action>, List<SyncControl.Action>> {
        val insertActions = syncActions.filter { it.action == SyncControl.ActionType.INSERT }
            .sortedBy { it.getActionedAtTimestamp() }
        val updateActions = syncActions.filter { it.action == SyncControl.ActionType.UPDATE }
            .sortedBy { it.getActionedAtTimestamp() }
        val deleteActions = syncActions.filter { it.action == SyncControl.ActionType.DELETE }
            .sortedBy { it.getActionedAtTimestamp() }
        return Triple(insertActions, updateActions, deleteActions)
    }
}
