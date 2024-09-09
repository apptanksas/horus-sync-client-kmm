package com.apptank.horus.client.sync.manager

import com.apptank.horus.client.auth.HorusAuthentication
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.data.InternalModel
import com.apptank.horus.client.data.toDTORequest
import com.apptank.horus.client.database.DatabaseOperation
import com.apptank.horus.client.database.IOperationDatabaseHelper
import com.apptank.horus.client.database.builder.SimpleQueryBuilder
import com.apptank.horus.client.database.SQL
import com.apptank.horus.client.database.toDeleteRecord
import com.apptank.horus.client.database.toInsertRecord
import com.apptank.horus.client.database.toRecordsInsert
import com.apptank.horus.client.database.toUpdateRecord
import com.apptank.horus.client.exception.UserNotAuthenticatedException
import com.apptank.horus.client.extensions.evaluate
import com.apptank.horus.client.extensions.isTrue
import com.apptank.horus.client.extensions.log
import com.apptank.horus.client.hashing.AttributeHasher
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.sync.network.dto.toDomain
import com.apptank.horus.client.sync.network.dto.toEntityData
import com.apptank.horus.client.sync.network.dto.toInternalModel
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SynchronizatorManager(
    private val netWorkValidator: INetworkValidator,
    private val syncControlDatabaseHelper: ISyncControlDatabaseHelper,
    private val operationDatabaseHelper: IOperationDatabaseHelper,
    private val synchronizationService: ISynchronizationService
) {

    enum class SynchronizationStatus {
        SUCCESS,
        IN_PROGRESS,
        FAILED,
        IDLE
    }

    /**
     * Start the validation process
     */
    suspend fun start(onStatus: (SynchronizationStatus, isCompleted: Boolean) -> Unit) {

        // Validate if there is network available
        if (!netWorkValidator.isNetworkAvailable()) {
            log("[DataValidatorManager] No network available")
            return onStatus(SynchronizationStatus.IDLE, true)
        }

        // Validate if there are pending actions to sync with the server
        if (syncControlDatabaseHelper.getPendingActions().isNotEmpty()) {
            log("[DataValidatorManager] There is pending actions")
            return onStatus(SynchronizationStatus.IDLE, true)
        }

        onStatus(SynchronizationStatus.IN_PROGRESS, false)

        // Stage 1: Validate if there are new data to sync with the server
        val validateIsExistsDataToSync =
            existsDataToSync() ?: return onStatus(SynchronizationStatus.FAILED, true)

        if (validateIsExistsDataToSync.isTrue()) {
            log("[DataValidatorManager] There are new data to sync with the server")

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

        entitiesHashesValidated.forEach {
            val entity = it.first
            val isHashCorrect = it.second
            // First -> Entity name, Second -> Validation result
            log("[DataValidatorManager] Entity: $entity - IsHashCorrect: $isHashCorrect")

            if (!isHashCorrect) {
                corruptedEntities[it.first] = validateEntityDataCorrupted(it.first)
            }
        }

        val resultsCorruptedData = mutableListOf<Boolean>()

        // Stage 3: Restore corrupted data
        corruptedEntities.forEach { (entity, ids) ->
            if (ids.isNotEmpty()) {
                val resultRestoreCorruptedData = restoreCorruptedData(entity, ids)
                if (resultRestoreCorruptedData) {
                    log("[DataValidatorManager][entity:$entity] Corrupted data restored successfully")
                } else {
                    log("[DataValidatorManager][entity:$entity] Error restoring corrupted data")
                }
                resultsCorruptedData.add(resultRestoreCorruptedData)
            } else {
                log("[DataValidatorManager][entity:$entity] No corrupted data to restore")
            }
        }

        return onStatus(
            resultsCorruptedData.all { it }.evaluate(
                SynchronizationStatus.SUCCESS,
                SynchronizationStatus.FAILED
            ), true
        )
    }


    private suspend fun existsDataToSync(): Boolean? {

        val checkpointTimestamp = syncControlDatabaseHelper.getLastDatetimeCheckpoint()
        val lastActions =
            syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp)

        val resultActions = synchronizationService.getQueueActions(
            checkpointTimestamp,
            lastActions.map { it.getActionedAtTimestamp() })

        when (resultActions) {
            is DataResult.Success -> {
                // If there are actions means that there is data to sync
                return resultActions.data.isNotEmpty()
            }

            is DataResult.Failure -> {
                resultActions.exception.printStackTrace()
                log("[DataValidatorManager] Error getting queue data")
            }

            is DataResult.NotAuthorized -> {
                log("[DataValidatorManager] Not authorized")
            }
        }

        return null
    }

    /**
     * Get the hashes of the entities
     *
     * @return List of entities hashes
     */
    private fun getEntityHashes(): List<Horus.EntityHash> {

        val output = mutableListOf<Horus.EntityHash>()

        syncControlDatabaseHelper.getEntityNames().forEach { entity ->
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
     * Validate the hashes of the entities with the server
     *
     * @param entitiesHashes List of entities hashes
     * @return List of entities with the validation result
     */
    private suspend fun validateEntityHashes(entitiesHashes: List<Horus.EntityHash>): List<Pair<String, Boolean>> {
        return validateRemoteEntitiesData(entitiesHashes).map { Pair(it.entity, it.isHashMatched) }
    }

    /**
     * Validate the data of an entity
     *
     * @param entity Entity to validate
     * @return List of ids with corrupted data
     */
    private suspend fun validateEntityDataCorrupted(entity: String): List<String> {
        val remoteHashes = getRemoteEntitiesHashes(entity)
        return compareEntityHashesWithLocalData(entity, remoteHashes)
    }

    /**
     * Compare the local hashes with the remote hashes
     *
     * @param entity Entity to compare
     * @param remoteHashes List of hashes from the server
     * @return List of ids with corrupted data
     */
    private fun compareEntityHashesWithLocalData(
        entity: String,
        remoteHashes: List<InternalModel.EntityIdHash>
    ): List<String> {

        val ids = mutableListOf<String>()

        val localIdsHashes = operationDatabaseHelper.queryRecords(
            SimpleQueryBuilder(entity).select(Horus.Attribute.ID, Horus.Attribute.HASH)
        ).associate { it[Horus.Attribute.ID].toString() to it[Horus.Attribute.HASH].toString() }

        remoteHashes.forEach {
            val localHash = localIdsHashes[it.id]
            val entityExists = localHash != null

            if (entityExists && localHash != it.hash) {
                log("[DataValidatorManager:compareEntityHashesWithLocalData] Problem detected integrity -> Entity: $entity - Id: ${it.id} - Local: $localHash - Remote: ${it.hash}")
                ids.add(it.id)
            }
        }

        return ids
    }


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
                    log("[DataValidatorManager] Error deleting corrupted data")
                    return false
                }

                return operationDatabaseHelper.insertWithTransaction(
                    dataEntitiesResponse.data.map { it.toEntityData() }
                        .flatMap { it.toRecordsInsert() })
            }

            is DataResult.Failure -> {
                dataEntitiesResponse.exception.printStackTrace()
                log("[DataValidatorManager] Error restoring corrupted data")
            }

            is DataResult.NotAuthorized -> {
                log("[DataValidatorManager] Not authorized")
            }
        }

        return false
    }

    private suspend fun synchronizeData(): Boolean {

        val checkpointDatetime = syncControlDatabaseHelper.getLastDatetimeCheckpoint()

        if (checkpointDatetime == 0L) {
            log("[DataValidatorManager] No checkpoint datetime")
            return true
        }

        log("[DataValidatorManager] Synchronizing data from checkpoint datetime: $checkpointDatetime")

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
                    log("[DataValidatorManager:synchronizeData] Data synchronized successfully")
                    SyncControl.Status.COMPLETED
                } else {
                    log("[DataValidatorManager:synchronizeData] Error synchronizing data")
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
                log("[DataValidatorManager] Error getting queue data")
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncControl.OperationType.CHECKPOINT,
                    SyncControl.Status.FAILED
                )
                return false
            }

            is DataResult.NotAuthorized -> {
                log("[DataValidatorManager] Not authorized")
                return false
            }
        }
    }

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

    private fun mapToInsertOperation(actions: List<SyncControl.Action>): List<DatabaseOperation.InsertRecord> {
        return actions.map { it.toInsertRecord(getUserId()) }
    }

    private fun mapToUpdateOperation(actions: List<SyncControl.Action>): List<DatabaseOperation.UpdateRecord> {

        val actionsUpdate = actions.mapNotNull {
            getEntityById(it.entity, it.getEntityId())?.let { entity ->
                it.toUpdateRecord(entity)
            } ?: run {
                log("[DataValidatorManager] Error updating data. [${it.data}]")
                null
            }
        }

        return actionsUpdate
    }

    private fun mapToDeleteOperation(actions: List<SyncControl.Action>): List<DatabaseOperation.DeleteRecord> {
        return actions.map { it.toDeleteRecord() }
    }

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

    private fun getUserId(): String {
        return HorusAuthentication.getUserAuthenticatedId() ?: throw UserNotAuthenticatedException()
    }

    private suspend fun getRemoteEntitiesHashes(entity: String): List<InternalModel.EntityIdHash> {
        when (val result = synchronizationService.getEntityHashes(entity)) {
            is DataResult.Success -> {
                return result.data.map { it.toInternalModel() }
            }

            is DataResult.Failure -> {
                result.exception.printStackTrace()
                log("[DataValidatorManager:getEntitiesHashes] Error validating entity data")
            }

            is DataResult.NotAuthorized -> {
                log("[DataValidatorManager:getEntitiesHashes] Not authorized")
            }
        }

        return emptyList()
    }

    private suspend fun validateRemoteEntitiesData(entitiesHashes: List<Horus.EntityHash>): List<InternalModel.EntityHashValidation> {
        when (val result =
            synchronizationService.postValidateEntitiesData(entitiesHashes.toDTORequest())) {
            is DataResult.Success -> {
                return result.data.map { it.toInternalModel() }
            }

            is DataResult.Failure -> {
                result.exception.printStackTrace()
                log("[DataValidatorManager:getRemoteValidateEntitiesData] Error validating entity data")
            }

            is DataResult.NotAuthorized -> {
                log("[DataValidatorManager:getRemoteValidateEntitiesData] Not authorized")
            }
        }

        return emptyList()
    }

    /**
     * Organize the actions by type
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