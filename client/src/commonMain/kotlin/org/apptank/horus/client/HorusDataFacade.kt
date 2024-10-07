package org.apptank.horus.client

import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.base.CallbackEvent
import org.apptank.horus.client.base.CallbackNullable
import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.data.DataChangeListener
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.DatabaseOperation
import org.apptank.horus.client.database.SQL
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.mapToDBColumValue
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.exception.EntityNotExistsException
import org.apptank.horus.client.exception.EntityNotWritableException
import org.apptank.horus.client.exception.UserNotAuthenticatedException
import org.apptank.horus.client.extensions.removeIf
import org.apptank.horus.client.tasks.ControlTaskManager
import org.apptank.horus.client.utils.AttributesPreparator
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Provides a facade for synchronizing data operations, including inserting, updating, and deleting entities.
 * Handles callbacks and data change listeners, and interacts with the database through various helpers.
 */
object HorusDataFacade {

    private var isReady = false
    private var onCallbackReady: Callback? = null

    private var changeListeners: MutableList<DataChangeListener> = mutableListOf()

    private val operationDatabaseHelper by lazy {
        HorusContainer.getOperationDatabaseHelper()
    }

    private val syncControlDatabaseHelper by lazy {
        HorusContainer.getSyncControlDatabaseHelper()
    }

    private val remoteSynchronizatorManager by lazy {
        HorusContainer.getRemoteSynchronizatorManager()
    }

    private val controlTaskManager by lazy { ControlTaskManager }

    private var networkValidator: INetworkValidator? = null
        get() {
            if (field == null) {
                field = HorusContainer.getNetworkValidator()
            }
            return field
        }

    init {
        registerEntityEventListeners()
        EventBus.register(EventType.ON_READY) {
            isReady = true
            onCallbackReady?.invoke()
        }
    }

    /**
     * Sets a callback to be invoked when the facade is ready for operations.
     *
     * @param callback The callback to be invoked when the facade is ready.
     */
    fun onReady(callback: Callback) {
        onCallbackReady = callback
    }

    /**
     * Inserts a new record into the database with specified attributes.
     *
     * @param entity The name of the entity to insert.
     * @param attributes The attributes to insert with the entity.
     * @return A [DataResult] indicating success or failure of the insert operation.
     */
    fun insert(entity: String, attributes: List<Horus.Attribute<*>>): DataResult<String> {

        validateConstraints(entity)

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

    /**
     * Inserts a new record into the database with specified attributes.
     *
     * @param entity The name of the entity to insert.
     * @param attributes The attributes to insert with the entity.
     * @return A [DataResult] indicating success or failure of the insert operation.
     */
    fun insert(
        entity: String,
        vararg attributes: Horus.Attribute<*>
    ): DataResult<String> {
        return insert(entity, attributes.toList())
    }

    /**
     * Inserts a new record into the database with specified attributes.
     *
     * @param entity The name of the entity to insert.
     * @param attributes The attributes to insert with the entity.
     * @return A [DataResult] indicating success or failure of the insert operation.
     */
    fun insert(
        entity: String,
        attributes: Map<String, Any>,
    ): DataResult<String> {
        return insert(entity, attributes.map { Horus.Attribute(it.key, it.value) })
    }

    /**
     * Updates an existing entity in the database with specified attributes.
     *
     * @param entity The name of the entity to update.
     * @param id The ID of the entity to update.
     * @param attributes The attributes to update for the entity.
     * @return A [DataResult] indicating success or failure of the update operation.
     */
    fun update(
        entity: String,
        id: String,
        attributes: List<Horus.Attribute<*>>
    ): DataResult<Unit> {

        validateConstraints(entity)

        if (AttributesPreparator.isAttributesNameContainsRestricted(attributes)) {
            return DataResult.Failure(IllegalStateException("Attribute restricted"))
        }
        val attrId = Horus.Attribute(Horus.Attribute.ID, id)
        val currentData = getById(entity, id) ?: return DataResult.Failure(
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

    /**
     * Updates an existing entity in the database with specified attributes.
     *
     * @param entity The name of the entity to update.
     * @param id The ID of the entity to update.
     * @param attributes The attributes to update for the entity.
     * @return A [DataResult] indicating success or failure of the update operation.
     */
    fun update(
        entity: String,
        id: String,
        vararg attributes: Horus.Attribute<*>
    ): DataResult<Unit> {
        return update(entity, id, attributes.toList())
    }

    /**
     * Updates an existing record in the database with specified attributes.
     *
     * @param entity The name of the entity to update.
     * @param id The ID of the entity to update.
     * @param attributes The attributes to update for the entity.
     * @return A [DataResult] indicating success or failure of the update operation.
     */
    fun update(
        entity: String,
        id: String,
        attributes: Map<String, Any>
    ): DataResult<Unit> {
        return update(entity, id, attributes.map { Horus.Attribute(it.key, it.value) })
    }

    /**
     * Deletes an existing record from the database.
     *
     * @param entity The name of the entity to delete.
     * @param id The ID of the entity to delete.
     * @return A [DataResult] indicating success or failure of the delete operation.
     */
    fun delete(entity: String, id: String): DataResult<Unit> {

        validateConstraints(entity)

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

    /**
     * Retrieves a list of records from the database based on the specified conditions.
     *
     * @param entity The name of the entity to retrieve.
     * @param conditions The conditions to apply to the query.
     * @param orderBy The column to order the results by.
     * @param limit The maximum number of results to return.
     * @param offset The number of results to skip before starting to return results.
     * @return A [DataResult] containing a list of [Horus.Entity] objects.
     */
    suspend fun querySimple(
        entity: String,
        conditions: List<SQL.WhereCondition> = listOf(),
        orderBy: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): DataResult<List<Horus.Entity>> {

        validateConstraintsReadable(entity)

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

    /**
     * Retrieves a single record from the database by its ID.
     *
     * @param entity The name of the entity to retrieve.
     * @param id The ID of the entity to retrieve.
     * @return A [Horus.Entity] object if found, or `null` if not found.
     */
    fun getById(entity: String, id: String): Horus.Entity? {

        validateConstraintsReadable(entity)

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

    /**
     * Gets the list of entities available in the database.
     */
    fun getEntityNames(): List<String> {
        return syncControlDatabaseHelper.getEntityNames()
    }

    /**
     * Forces a synchronization of data with the remote server. Validating if the network is available.
     *
     * @param onSuccess The callback to be invoked when the synchronization is successful.
     * @param onFailure The callback to be invoked when the synchronization fails.
     */
    fun forceSync(onSuccess: CallbackNullable = null, onFailure: CallbackNullable = null) {

        if (networkValidator?.isNetworkAvailable() == false) {
            onFailure?.invoke()
            return
        }

        var callbackSyncPushSuccess: CallbackEvent? = null
        var callbackSyncPushFailure: CallbackEvent? = null
        val removeListeners: Callback = {
            callbackSyncPushSuccess?.let { callback ->
                EventBus.unregister(
                    EventType.SYNC_PUSH_SUCCESS,
                    callback
                )
            }
            callbackSyncPushFailure?.let { callback ->
                EventBus.unregister(
                    EventType.SYNC_PUSH_FAILED,
                    callback
                )
            }
        }
        callbackSyncPushSuccess = { onSuccess?.invoke();removeListeners.invoke() }
        callbackSyncPushFailure = { onFailure?.invoke();removeListeners.invoke() }

        with(controlTaskManager) {
            setOnCallbackStatusListener {
                if (it === ControlTaskManager.Status.FAILED) {
                    onFailure?.invoke()
                }
            }
            setOnCompleted {

                EventBus.register(EventType.SYNC_PUSH_SUCCESS, callbackSyncPushSuccess)
                EventBus.register(EventType.SYNC_PUSH_FAILED, callbackSyncPushFailure)

                remoteSynchronizatorManager.trySynchronizeData()
            }
            start()
        }
    }

    /**
     * Adds a listener to be notified of data changes.
     *
     * @param dataChangeListener The listener to add.
     */
    fun addDataChangeListener(dataChangeListener: DataChangeListener) {
        changeListeners.add(dataChangeListener)
    }

    /**
     * Removes a specific data change listener.
     *
     * @param dataChangeListener The listener to remove.
     */
    fun removeDataChangeListener(dataChangeListener: DataChangeListener) {
        changeListeners.remove(dataChangeListener)
    }

    /**
     * Removes all data change listeners.
     */
    fun removeAllDataChangeListeners() {
        changeListeners.clear()
    }


    /**
     * Validates the constraints for the facade.
     */
    private fun validateConstraints(entity: String) {
        validateIsReady()
        validateIsEntityExists(entity)
        validateIsCanWriteIntoEntity(entity)
    }

    /**
     * Validates the constraints for the facade.
     */
    private fun validateConstraintsReadable(entity: String) {
        validateIsReady()
        validateIsEntityExists(entity)
    }

    /**
     * Checks if the facade is ready for operations and throws an exception if not.
     */
    private fun validateIsReady() {
        if (!isReady) {
            throw IllegalStateException("Synchronizer not ready")
        }
    }

    /**
     * Validates if the entity exists in the database.
     */
    private fun validateIsEntityExists(entity: String) {
        syncControlDatabaseHelper.getEntityNames().find { it == entity }
            ?: throw EntityNotExistsException(entity)
    }

    private fun validateIsCanWriteIntoEntity(entity: String) {
        if (syncControlDatabaseHelper.isEntityCanBeWritable(entity)) {
            return
        }
        throw EntityNotWritableException(entity)
    }

    /**
     * Registers event listeners for entity creation, update, and deletion.
     */
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

    /**
     * Retrieves the authenticated user ID.
     *
     * @return The user ID of the authenticated user.
     * @throws UserNotAuthenticatedException If no user is authenticated.
     */
    private fun getUserId(): String {
        return HorusAuthentication.getUserAuthenticatedId() ?: throw UserNotAuthenticatedException()
    }

    /**
     * Generates a new UUID.
     *
     * @return A new UUID as a [String].
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun generateUUID(): String {
        return Uuid.random().toString()
    }

    /**
     * Clears the state of the facade, resetting readiness and clearing listeners.
     */
    internal fun clear() {
        isReady = false
        onCallbackReady = null
        changeListeners.clear()
        networkValidator = null
    }
}
