package com.apptank.horus.client

import com.apptank.horus.client.auth.HorusAuthentication
import com.apptank.horus.client.base.Callback
import com.apptank.horus.client.base.DataMap
import com.apptank.horus.client.base.DataResult
import com.apptank.horus.client.data.DataChangeListener
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.database.DatabaseOperation
import com.apptank.horus.client.database.SQL
import com.apptank.horus.client.database.builder.SimpleQueryBuilder
import com.apptank.horus.client.database.mapToDBColumValue
import com.apptank.horus.client.di.HorusContainer
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import com.apptank.horus.client.exception.UserNotAuthenticatedException
import com.apptank.horus.client.extensions.removeIf
import com.apptank.horus.client.utils.AttributesPreparator
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object SynchronizeDataFacade {

    private var isReady = false
    private var onCallbackReady: Callback? = null

    private var changeListeners: MutableList<DataChangeListener> = mutableListOf()

    private val operationDatabaseHelper by lazy {
        HorusContainer.getOperationDatabaseHelper()
    }

    private val syncControlDatabaseHelper by lazy {
        HorusContainer.getSyncControlDatabaseHelper()
    }

    init {
        registerEntityEventListeners()
        EventBus.register(EventType.VALIDATION_COMPLETED) {
            isReady = true
            onCallbackReady?.invoke()
        }
    }

    fun onReady(callback: Callback) {
        onCallbackReady = callback
    }

    fun insert(entity: String, attributes: List<Horus.Attribute<*>>): DataResult<String> {

        validateIfReady()

        if (AttributesPreparator.isAttributesNameContainsRestricted(attributes)) {
            return DataResult.Failure(IllegalStateException("Attribute restricted"))
        }

        val uuid = generateUUID()
        val id = Horus.Attribute(Horus.Attribute.ID, uuid)

        val attributesPrepared = AttributesPreparator.appendHashAndUpdateAttributes(
            id,
            AttributesPreparator.appendInsertSyncAttributes(id, attributes, getUserId())
        )

        return runCatching {
            val result = operationDatabaseHelper.insertWithTransaction(
                listOf(
                    DatabaseOperation.InsertRecord(
                        entity, attributesPrepared.mapToDBColumValue()
                    )
                )
            ) {
                syncControlDatabaseHelper.addActionInsert(
                    entity,
                    mutableListOf<Horus.Attribute<*>>(id).apply { addAll(attributes) })
            }

            if (result) {
                return@runCatching DataResult.Success(uuid)
            }
            return@runCatching DataResult.Failure(IllegalStateException("Insert entity failure"))
        }.getOrElse {
            DataResult.Failure(it)
        }
    }

    fun insert(
        entity: String,
        vararg attributes: Horus.Attribute<*>
    ): DataResult<String> {
        return insert(entity, attributes.toList())
    }

    fun insert(
        entity: String,
        attributes: Map<String, Any>,
    ): DataResult<String> {
        return insert(entity, attributes.map { Horus.Attribute(it.key, it.value) })
    }

    fun updateEntity(
        entity: String,
        id: String,
        attributes: List<Horus.Attribute<*>>
    ): DataResult<Unit> {

        validateIfReady()

        if (AttributesPreparator.isAttributesNameContainsRestricted(attributes)) {
            return DataResult.Failure(IllegalStateException("Attribute restricted"))
        }
        val attrId = Horus.Attribute(Horus.Attribute.ID, id)
        val currentData = getEntityById(entity, id) ?: return DataResult.Failure(
            IllegalStateException("Entity not found")
        )

        // Append hash and update attributes
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



        return runCatching {
            val result = operationDatabaseHelper.updateWithTransaction(
                listOf(
                    DatabaseOperation.UpdateRecord(
                        entity, attributesPrepared.mapToDBColumValue(),
                        listOf(
                            SQL.WhereCondition(
                                SQL.ColumnValue(Horus.Attribute.ID, id),
                                SQL.Comparator.EQUALS
                            )
                        )
                    )
                )
            ) {
                syncControlDatabaseHelper.addActionUpdate(
                    entity,
                    attrId,
                    attributes
                )
            }

            if (result) {
                return@runCatching DataResult.Success(Unit)
            }
            return@runCatching DataResult.Failure(IllegalStateException("Update entity failure"))
        }.getOrElse {
            DataResult.Failure(it)
        }
    }


    fun updateEntity(
        entity: String,
        id: String,
        vararg attributes: Horus.Attribute<*>
    ): DataResult<Unit> {
        return updateEntity(entity, id, attributes.toList())
    }

    fun updateEntity(
        entity: String,
        id: String,
        attributes: Map<String, Any>
    ): DataResult<Unit> {
        return updateEntity(entity, id, attributes.map { Horus.Attribute(it.key, it.value) })
    }

    fun deleteEntity(entity: String, id: String): DataResult<Unit> {

        validateIfReady()

        val attrId = Horus.Attribute(Horus.Attribute.ID, id)

        return runCatching {
            val result = operationDatabaseHelper.deleteWithTransaction(
                listOf(
                    DatabaseOperation.DeleteRecord(
                        entity,
                        listOf(
                            SQL.WhereCondition(
                                SQL.ColumnValue(Horus.Attribute.ID, id),
                                SQL.Comparator.EQUALS
                            )
                        )
                    )
                )
            ) {
                syncControlDatabaseHelper.addActionDelete(entity, attrId)
            }

            if (result) {
                return@runCatching DataResult.Success(Unit)
            }
            return@runCatching DataResult.Failure(IllegalStateException("Delete entity failure"))
        }.getOrElse {
            DataResult.Failure(it)
        }
    }

    suspend fun getEntities(
        entity: String,
        conditions: List<SQL.WhereCondition> = listOf(),
        orderBy: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): DataResult<List<Horus.Entity>> {

        validateIfReady()

        val queryBuilder = SimpleQueryBuilder(entity).apply {
            where(*conditions.toTypedArray())
            orderBy?.let {
                orderBy(it)
            }
            limit?.let {
                limit(it)
            }
            offset?.let {
                offset(it)
            }
        }

        val result = operationDatabaseHelper.queryRecords(queryBuilder).map {
            Horus.Entity(
                entity,
                it.map { Horus.Attribute(it.key, it.value) }
            )
        }

        return DataResult.Success(result)
    }

    fun getEntityById(entity: String, id: String): Horus.Entity? {

        validateIfReady()

        val queryBuilder = SimpleQueryBuilder(entity).apply {
            where(
                SQL.WhereCondition(
                    SQL.ColumnValue(Horus.Attribute.ID, id),
                    SQL.Comparator.EQUALS
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

    fun addDataChangeListener(dataChangeListener: DataChangeListener) {
        changeListeners.add(dataChangeListener)
    }

    fun removeDataChangeListener(dataChangeListener: DataChangeListener) {
        changeListeners.remove(dataChangeListener)
    }

    fun removeAllDataChangeListeners() {
        changeListeners.clear()
    }

    private fun validateIfReady() {
        if (!isReady) {
            throw IllegalStateException("Synchronizer not ready")
        }
    }

    private fun registerEntityEventListeners() {

        // Notify listeners when an entity is created
        EventBus.register(EventType.ENTITY_CREATED) {
            changeListeners.forEach { listener ->

                val entity = it.data?.get("entity") as? String ?: ""
                val attributes = it.data?.get("attributes") as? DataMap ?: mapOf()

                it.data?.get(Horus.Attribute.ID)?.let {
                    listener.onInsert(entity, it as String, attributes)
                }
            }
        }

        // Notify listeners when an entity is updated
        EventBus.register(EventType.ENTITY_UPDATED) {
            changeListeners.forEach { listener ->
                val entity = it.data?.get("entity") as? String ?: ""
                val attributes = it.data?.get("attributes") as? DataMap ?: mapOf()
                it.data?.get(Horus.Attribute.ID)?.let {
                    listener.onUpdate(entity, it as String, attributes)
                }
            }
        }

        // Notify listeners when an entity is deleted
        EventBus.register(EventType.ENTITY_DELETED) {
            changeListeners.forEach { listener ->
                val entity = it.data?.get("entity") as? String ?: ""
                it.data?.get(Horus.Attribute.ID)?.let {
                    listener.onDelete(entity, it as String)
                }
            }
        }
    }

    private fun getUserId(): String {
        return HorusAuthentication.getUserAuthenticatedId() ?: throw UserNotAuthenticatedException()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateUUID(): String {
        return Uuid.random().toByteArray().toString()
    }

    internal fun clear() {
        isReady = false
        onCallbackReady = null
        changeListeners.clear()
    }

}