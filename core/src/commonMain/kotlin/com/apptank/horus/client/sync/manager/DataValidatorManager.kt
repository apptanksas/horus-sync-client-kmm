package com.apptank.horus.client.sync.manager

import com.apptank.horus.client.auth.HorusAuthentication
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.control.ControlStatus
import com.apptank.horus.client.control.ISyncControlDatabaseHelper
import com.apptank.horus.client.control.SyncOperationType
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.database.IOperationDatabaseHelper
import com.apptank.horus.client.database.builder.SimpleQueryBuilder
import com.apptank.horus.client.database.mapToDBColumValue
import com.apptank.horus.client.database.DatabaseOperation
import com.apptank.horus.client.database.SQL
import com.apptank.horus.client.database.toRecordsInsert
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.exception.UserNotAuthenticatedException
import com.apptank.horus.client.extensions.log
import com.apptank.horus.client.extensions.removeIf
import com.apptank.horus.client.hashing.AttributeHasher
import com.apptank.horus.client.interfaces.INetworkValidator
import com.apptank.horus.client.sync.network.dto.EntityIdHashDTO
import com.apptank.horus.client.sync.network.dto.SyncActionResponse
import com.apptank.horus.client.sync.network.dto.SyncDTO
import com.apptank.horus.client.sync.network.dto.toEntityData
import com.apptank.horus.client.sync.network.service.ISynchronizationService
import com.apptank.horus.client.utils.AttributesPreparator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DataValidatorManager(
    private val netWorkValidator: INetworkValidator,
    private val syncControlDatabaseHelper: ISyncControlDatabaseHelper,
    private val operationDatabaseHelper: IOperationDatabaseHelper,
    private val synchronizationService: ISynchronizationService,
    private val event: EventBus,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {


    /**
     * Start the validation process
     */
    fun start() {

        // Validate if there is network available
        if (!netWorkValidator.isNetworkAvailable()) {
            log("[SyncValidator] No network available")
            return
        }

        // Validate if there are pending actions to sync with the server
        if (syncControlDatabaseHelper.getPendingActions().isNotEmpty()) {
            log("[SyncValidator] There is pending actions")
            return
        }

        CoroutineScope(dispatcher).apply {

            launch {

                // Stage 1: Validate if there are new data to sync with the server

                if (existsDataToSync()) {
                    log("[SyncValidator] There are new data to sync with the server")
                    synchronizeData()
                    return@launch
                }

                // Stage 2: Validate entity hashes

                val entitiesHashes = getEntityHashes()
                val entitiesHashesValidated = validateEntityHashes(entitiesHashes)
                // EntityName -> List of corrupted ids
                val corruptedEntities = mutableMapOf<String, List<String>>()

                entitiesHashesValidated.forEach {
                    // First -> Entity name, Second -> Validation result
                    log("[SyncValidator] Entity: ${it.first} - Validated: ${it.second}")

                    if (!it.second) {
                        corruptedEntities[it.first] = validateEntityDataCorrupted(it.first)
                    }
                }

                // Stage 3: Restore corrupted data
                corruptedEntities.forEach { (entity, ids) ->

                    if (ids.isNotEmpty()) {
                        if (restoreCorruptedData(entity, ids)) {
                            log("[SyncValidator][entity:$entity] Corrupted data restored successfully")
                        } else {
                            log("[SyncValidator][entity:$entity] Error restoring corrupted data")
                        }
                    } else {
                        log("[SyncValidator][entity:$entity] No corrupted data to restore")
                    }
                }

            }.invokeOnCompletion {
                it?.printStackTrace()
                cancel()
            }
        }
    }


    private suspend fun existsDataToSync(): Boolean {

        val checkpointTimestamp = syncControlDatabaseHelper.getLastDatetimeCheckpoint()
        val lastActions =
            syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp)

        val resultActions = synchronizationService.getQueueActions(
            checkpointTimestamp,
            lastActions.map { it.getDatetimeAsTimestamp() })

        when (resultActions) {
            is DataResult.Success -> {
                // If there are actions means that there is data to sync
                return resultActions.data.isNotEmpty()
            }

            is DataResult.Failure -> {
                resultActions.exception.printStackTrace()
                log("[SyncValidator] Error getting queue data")
            }

            is DataResult.NotAuthorized -> {
                log("[SyncValidator] Not authorized")
            }
        }

        return false
    }

    /**
     * Get the hashes of the entities
     *
     * @return List of entities hashes
     */
    private fun getEntityHashes(): List<SyncDTO.EntityHash> {

        val output = mutableListOf<SyncDTO.EntityHash>()

        syncControlDatabaseHelper.getEntityNames().forEach { entity ->
            val hashes = mutableListOf<String>()
            // Create a query to get the id and sync_hash of the entity
            val queryBuilder = SimpleQueryBuilder(entity)
            queryBuilder.select("sync_hash").orderBy("id")

            // Execute the query and get the hashes
            operationDatabaseHelper.queryRecords(queryBuilder).forEach { record ->
                hashes.add(record["sync_hash"] as String)
            }

            if (hashes.isNotEmpty()) {
                output.add(SyncDTO.EntityHash(entity, AttributeHasher.generateHashFromList(hashes)))
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
    private suspend fun validateEntityHashes(entitiesHashes: List<SyncDTO.EntityHash>): List<Pair<String, Boolean>> {
        val result = synchronizationService.postValidateEntitiesData(entitiesHashes)
        when (result) {
            is DataResult.Success -> {
                return result.data.map { Pair(it.entity!!, it.hash?.matched ?: false) }
            }

            is DataResult.Failure -> {
                result.exception.printStackTrace()
                log("[SyncValidator] Error validating entity hashes")
            }

            is DataResult.NotAuthorized -> {
                log("[SyncValidator] Not authorized")
            }
        }
        return emptyList()
    }

    /**
     * Validate the data of an entity
     *
     * @param entity Entity to validate
     * @return List of ids with corrupted data
     */
    private suspend fun validateEntityDataCorrupted(entity: String): List<String> {

        val result = synchronizationService.getEntityHashes(entity)

        when (result) {
            is DataResult.Success -> {
                return compareEntityHashesWithLocalData(entity, result.data)
            }

            is DataResult.Failure -> {
                result.exception.printStackTrace()
                log("[SyncValidator] Error validating entity data")
            }

            is DataResult.NotAuthorized -> {
                log("[SyncValidator] Not authorized")
            }
        }

        return emptyList()
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
        remoteHashes: List<EntityIdHashDTO>
    ): List<String> {

        val ids = mutableListOf<String>()

        val localIdsHashes = operationDatabaseHelper.queryRecords(
            SimpleQueryBuilder(entity).select("id", "sync_hash")
        ).associate { it["id"].toString() to it["sync_hash"].toString() }

        remoteHashes.forEach {
            val localHash = localIdsHashes[it.id.toString()]
            val entityExists = localHash != null

            if (entityExists && localHash != it.hash) {
                log("[SyncValidator] Problem detected integrity -> Entity: $entity - Id: ${it.id} - Local: $localHash - Remote: ${it.hash}")
                ids.add(it.id.toString())
            }
        }

        return ids
    }


    private suspend fun restoreCorruptedData(entity: String, ids: List<String>): Boolean {
        val dataEntitiesResponse = synchronizationService.getDataEntity(entity, ids = ids)

        when (dataEntitiesResponse) {

            is DataResult.Success -> {
                // Delete ids with corrupted data
                val result = operationDatabaseHelper.deleteRecord(
                    entity,
                    ids.map { SQL.WhereCondition(SQL.ColumnValue("id", it)) },
                    SQL.LogicOperator.OR
                )

                if (!result.isSuccess) {
                    log("[SyncValidator] Error deleting corrupted data")
                    return false
                }

                return operationDatabaseHelper.insertTransaction(
                    dataEntitiesResponse.data.map { it.toEntityData() }
                        .flatMap { it.toRecordsInsert() })
            }

            is DataResult.Failure -> {
                dataEntitiesResponse.exception.printStackTrace()
                log("[SyncValidator] Error restoring corrupted data")
            }

            is DataResult.NotAuthorized -> {
                log("[SyncValidator] Not authorized")
            }
        }

        return false
    }

    private suspend fun synchronizeData() {
        val checkpointDatetime = syncControlDatabaseHelper.getLastDatetimeCheckpoint()

        if (checkpointDatetime == 0L) {
            log("[SyncValidator] No checkpoint datetime")
            return
        }

        log("[SyncValidator] Synchronizing data from checkpoint datetime: $checkpointDatetime")

        val actions = synchronizationService.getQueueActions(checkpointDatetime)

        when (actions) {
            is DataResult.Success -> {

                val newActions = filterOwnActions(actions.data, checkpointDatetime)

                newActions.forEach {
                    when (it.action) {
                        "INSERT" -> insertData(it)
                        "UPDATE" -> updateData(it)
                        "DELETE" -> deleteData(it)
                    }
                }
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncOperationType.CHECKPOINT,
                    ControlStatus.COMPLETED
                )
                log("[SyncValidator] Data synchronized successfully")
            }

            is DataResult.Failure -> {
                actions.exception.printStackTrace()
                log("[SyncValidator] Error getting queue data")
                syncControlDatabaseHelper.addSyncTypeStatus(
                    SyncOperationType.CHECKPOINT,
                    ControlStatus.FAILED
                )
            }

            is DataResult.NotAuthorized -> {
                log("[SyncValidator] Not authorized")
            }
        }
    }

    private fun filterOwnActions(
        actions: List<SyncActionResponse>,
        checkpointTimestamp: Long
    ): List<SyncActionResponse> {

        val ownActions =
            syncControlDatabaseHelper.getCompletedActionsAfterDatetime(checkpointTimestamp)

        // Filter the actions that are not in the local database
        return actions.filterNot { action ->
            ownActions.find { it.getDatetimeAsTimestamp() == action.actionedAt!!.toLong() } != null
        }
    }

    private fun insertData(actionDTO: SyncActionResponse) {

        val id = Horus.Attribute("id", actionDTO.data?.get("id") as String)
        val attributes =
            actionDTO.data.filterNot { it.key == "id" }.map { Horus.Attribute(it.key, it.value) }
                .toList()

        val attributesPrepared = AttributesPreparator.appendHashAndUpdateAttributes(
            id,
            AttributesPreparator.appendInsertSyncAttributes(
                id,
                attributes,
                getUserId(),
                actionDTO.actionedAt!!.toLong()
            ), actionDTO.actionedAt!!.toLong()
        )

        runCatching {
            val result = operationDatabaseHelper.insertTransaction(
                listOf(
                    DatabaseOperation.InsertRecord(
                        actionDTO.entity!!, attributesPrepared.mapToDBColumValue()
                    )
                )
            )
            if (result) {
                log("[SyncValidator] Data inserted successfully. [${actionDTO.data}]")
            } else {
                log("[SyncValidator] Error inserting data. [${actionDTO.data}]")
            }
        }.getOrElse {
            it.printStackTrace()
            log("[SyncValidator] Error inserting data. [${actionDTO.data}]")
        }
    }

    private fun updateData(actionDTO: SyncActionResponse) {
        val id = actionDTO.data?.get("id") as String
        val entity = actionDTO.entity
        val attributes: List<Horus.Attribute<*>> =
            (actionDTO.data["attributes"] as Map<String, Any>).map {
                Horus.Attribute(
                    it.key,
                    it.value
                )
            }
                .toList()


        val currentData = getEntityById(entity!!, id)
            ?: return log("[SyncValidator] Error getting data to update")
        val attrId = Horus.Attribute("id", id)

        val attributesPrepared =
            AttributesPreparator.appendHashAndUpdateAttributes(
                attrId,
                attributes.toMutableList().apply {
                    currentData.attributes.filter { currentAttribute -> attributes.find { it.name == currentAttribute.name } == null }
                        .forEach {
                            add(it)
                        }
                    removeIf { it.name == "sync_hash" }
                })

        runCatching {
            val result = operationDatabaseHelper.updateRecordTransaction(
                listOf(
                    DatabaseOperation.UpdateRecord(
                        entity, attributesPrepared.mapToDBColumValue(),
                        listOf(SQL.WhereCondition(SQL.ColumnValue("id", id)))
                    )
                )
            )
            if (result) {
                log("[SyncValidator] Data updated successfully. [$actionDTO]")
            } else {
                log("[SyncValidator] Error updating data. [$actionDTO]")
            }
        }.getOrElse {
            it.printStackTrace()
            log("[SyncValidator] Error updating data. [$actionDTO]")
        }
    }

    private fun deleteData(actionDTO: SyncActionResponse) {

        // TODO("Validar otras entidades hijas para poder eliminarlas de forma local, por ejemplo: Si es un animal-> eliminar las pesos asociados")

        val id = actionDTO.data?.get("id") as String
        val result = operationDatabaseHelper.deleteRecord(
            actionDTO.entity!!,
            listOf(SQL.WhereCondition(SQL.ColumnValue("id", id)))
        )
        if (result.isSuccess) {
            log("[SyncValidator] Data deleted successfully. [${actionDTO.data}]")
        } else {
            log("[SyncValidator] Error deleting data. [${actionDTO.data}]")
        }
    }

    private fun getEntityById(entity: String, id: String): Horus.Entity? {

        val queryBuilder = SimpleQueryBuilder(entity).apply {
            where(
                SQL.WhereCondition(
                    SQL.ColumnValue("id", id)
                )
            )
        }

        return operationDatabaseHelper.queryRecords(queryBuilder).map {
            Horus.Entity(
                entity,
                it.map { Horus.Attribute(it.key, it.value) }
            )
        }?.firstOrNull()
    }

    private fun getUserId(): String {
        return HorusAuthentication.getUserAuthenticatedId() ?: throw UserNotAuthenticatedException()
    }

}