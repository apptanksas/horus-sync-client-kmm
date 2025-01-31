package org.apptank.horus.client

import org.apptank.horus.client.auth.HorusAuthentication
import org.apptank.horus.client.base.Callback
import org.apptank.horus.client.base.CallbackEvent
import org.apptank.horus.client.base.CallbackNullable
import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.base.DataResult
import org.apptank.horus.client.cache.MemoryCache
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.data.DataChangeListener
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.struct.DatabaseOperation
import org.apptank.horus.client.control.helper.IOperationDatabaseHelper
import org.apptank.horus.client.database.builder.QueryBuilder
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.database.struct.mapToDBColumValue
import org.apptank.horus.client.di.HorusContainer
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.eventbus.Event
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import org.apptank.horus.client.exception.EntityNotExistsException
import org.apptank.horus.client.exception.EntityNotWritableException
import org.apptank.horus.client.exception.OperationNotPermittedException
import org.apptank.horus.client.exception.UserNotAuthenticatedException
import org.apptank.horus.client.extensions.isFalse
import org.apptank.horus.client.extensions.removeIf
import org.apptank.horus.client.restrictions.EntityRestriction
import org.apptank.horus.client.sync.manager.ISyncFileUploadedManager
import org.apptank.horus.client.sync.manager.RemoteSynchronizatorManager
import org.apptank.horus.client.sync.upload.data.FileData
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository
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
    private var isInitialized = false
    private var onCallbackReady: Callback? = null

    private var changeListeners: MutableList<DataChangeListener> = mutableListOf()

    private var operationDatabaseHelper: IOperationDatabaseHelper? = null
        get() {
            if (field == null) {
                field = HorusContainer.getOperationDatabaseHelper()
            }
            return field
        }

    private var syncControlDatabaseHelper: ISyncControlDatabaseHelper? = null
        get() {
            if (field == null) {
                field = HorusContainer.getSyncControlDatabaseHelper()
            }
            return field
        }

    private var remoteSynchronizatorManager: RemoteSynchronizatorManager? = null
        get() {
            if (field == null) {
                field = HorusContainer.getRemoteSynchronizatorManager()
            }
            return field
        }

    private var syncFileUploadedManager: ISyncFileUploadedManager? = null
        get() {
            if (field == null) {
                field = HorusContainer.getSyncFileUploadedManager()
            }
            return field
        }

    private var uploadFileRepository: IUploadFileRepository? = null
        get() {
            if (field == null) {
                field = HorusContainer.getUploadFileRepository()
            }
            return field
        }


    private val controlTaskManager by lazy { ControlTaskManager }

    private var networkValidator: INetworkValidator? = null
        get() {
            if (field == null) {
                field = HorusContainer.getNetworkValidator()
            }
            return field
        }

    private val entityRestrictionValidator by lazy { HorusContainer.getEntityRestrictionValidator() }

    init {
        registerEntityEventListeners()
        registerObserverEvents()
        EventBus.register(EventType.ON_READY) {
            isReady = true
            onCallbackReady?.invoke()
        }
        isInitialized = true
    }

    /**
     * Initializes the facade.
     * This class is a singleton and should be initialized before use.
     */
    internal fun init() {
        if (isInitialized) {
            return
        }
    }

    /**
     * Sets a callback to be invoked when the facade is ready for operations.
     *
     * @param callback The callback to be invoked when the facade is ready.
     */
    fun onReady(callback: Callback) {
        onCallbackReady = callback

        if (isReady) {
            onCallbackReady?.invoke()
        }
    }

    /**
     * Sets the entity restrictions for the facade.
     *
     * @param restrictions The list of entity restrictions to set.
     */
    fun setEntityRestrictions(restrictions: List<EntityRestriction>) {
        entityRestrictionValidator.setRestrictions(restrictions)
    }

    /**
     * Inserts multiple records into the database with specified attributes.
     *
     * @param batch The list of batch to insert.
     *
     * @return A [DataResult] indicating success or failure of the insert operation with inserted IDs.
     */
    fun insertBatch(batch: List<Horus.Batch.Insert>): DataResult<List<String>> {

        val recordInserts = mutableListOf<DatabaseOperation.InsertRecord>()
        // ID, entity, attributes
        val insertIds =
            mutableListOf<Triple<Horus.Attribute<String>, String, List<Horus.Attribute<*>>>>()

        // Start validation
        entityRestrictionValidator.startValidation()

        batch.forEach { it ->

            val entity = it.entity
            val attributes = it.attributes

            validateConstraintsEntity(entity)
            try {
                entityRestrictionValidator.validate(entity, EntityRestriction.OperationType.INSERT)
            } catch (e: OperationNotPermittedException) {
                return DataResult.NotAuthorized(e)
            }

            if (AttributesPreparator.isAttributesNameContainsRestricted(attributes)) {
                return DataResult.Failure(IllegalStateException("Attribute restricted"))
            }

            val uuid = it.getAttribute<String>(Horus.Attribute.ID) ?: generateUUID()
            val id = Horus.Attribute(Horus.Attribute.ID, uuid)

            val attributesPrepared = AttributesPreparator.appendHashAndUpdateAttributes(
                id,
                AttributesPreparator.appendInsertSyncAttributes(id, attributes, getEffectiveUserId())
            )

            recordInserts.add(
                DatabaseOperation.InsertRecord(
                    entity,
                    attributesPrepared.mapToDBColumValue()
                )
            )
            insertIds.add(Triple(id, entity, attributes))
        }

        // Finish validation
        entityRestrictionValidator.finishValidation()

        val onInsertActions: Callback = {

            insertIds.forEach {

                val id = it.first
                val entity = it.second
                val attributes = it.third

                syncControlDatabaseHelper!!.addActionInsert(
                    entity,
                    mutableListOf<Horus.Attribute<*>>(id).apply { addAll(attributes) }
                )
            }
        }

        return runCatching {
            val result =
                operationDatabaseHelper!!.insertWithTransaction(recordInserts, onInsertActions)

            if (result) {
                return@runCatching DataResult.Success(insertIds.map { it.first.value })
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
    fun insert(entity: String, attributes: List<Horus.Attribute<*>>): DataResult<String> {

        validateConstraintsEntity(entity)

        // Validate entity restrictions
        try {
            with(entityRestrictionValidator) {
                startValidation()
                validate(entity, EntityRestriction.OperationType.INSERT)
                entityRestrictionValidator.finishValidation()
            }
        } catch (e: OperationNotPermittedException) {
            return DataResult.NotAuthorized(e)
        }

        if (AttributesPreparator.isAttributesNameContainsRestricted(attributes)) {
            return DataResult.Failure(IllegalStateException("Attribute restricted"))
        }

        val uuid =
            attributes.find { it.name == Horus.Attribute.ID }?.value as? String ?: generateUUID()
        val id = Horus.Attribute(Horus.Attribute.ID, uuid)

        val attributesPrepared = AttributesPreparator.appendHashAndUpdateAttributes(
            id,
            AttributesPreparator.appendInsertSyncAttributes(id, attributes, getEffectiveUserId())
        )

        return runCatching {
            val result = operationDatabaseHelper!!.insertWithTransaction(
                listOf(
                    DatabaseOperation.InsertRecord(
                        entity, attributesPrepared.mapToDBColumValue()
                    )
                )
            ) {
                syncControlDatabaseHelper!!.addActionInsert(
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
        attributes: Map<String, Any?>,
    ): DataResult<String> {
        return insert(entity, attributes.map { Horus.Attribute(it.key, it.value) })
    }

    /**
     * Updates multiple records in the database with specified attributes.
     *
     * @param batch The list of batch to update.
     *
     * @return A [DataResult] indicating success or failure of the update operation.
     */
    fun updateBatch(batch: List<Horus.Batch.Update>): DataResult<Unit> {

        val recordUpdates = mutableListOf<DatabaseOperation.UpdateRecord>()
        val updateIds =
            mutableListOf<Triple<Horus.Attribute<String>, String, List<Horus.Attribute<*>>>>()

        // Prepare update actions
        batch.forEach { it ->

            val entity = it.entity
            val id = it.id
            val attributes = it.attributes

            validateConstraintsEntity(entity)

            if (AttributesPreparator.isAttributesNameContainsRestricted(attributes)) {
                return DataResult.Failure(IllegalStateException("Attribute restricted"))
            }

            val attrId = Horus.Attribute(Horus.Attribute.ID, id)
            val currentData = getById(entity, id) ?: return DataResult.Failure(
                IllegalStateException("Entity not found")
            )

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

            recordUpdates.add(
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
            updateIds.add(Triple(attrId, entity, attributes))
        }

        // Add update actions to sync control
        val onUpdateActions: Callback = {

            updateIds.forEach {

                val id = it.first
                val entity = it.second
                val attributes = it.third

                syncControlDatabaseHelper!!.addActionUpdate(
                    entity,
                    id,
                    attributes
                )
            }
        }

        return runCatching {
            val result =
                operationDatabaseHelper!!.updateWithTransaction(recordUpdates, onUpdateActions)

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
        attributes: List<Horus.Attribute<*>>
    ): DataResult<Unit> {

        validateConstraintsEntity(entity)

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
            val result = operationDatabaseHelper!!.updateWithTransaction(
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
                syncControlDatabaseHelper!!.addActionUpdate(
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
        attributes: Map<String, Any?>
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

        validateConstraintsEntity(entity)

        val attrId = Horus.Attribute(Horus.Attribute.ID, id)

        return runCatching {
            val result = operationDatabaseHelper!!.deleteWithTransaction(
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
                syncControlDatabaseHelper!!.addActionDelete(entity, attrId)
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

        val result = operationDatabaseHelper!!.queryRecords(queryBuilder).map {
            Horus.Entity(
                entity,
                it.map { Horus.Attribute(it.key, it.value) }
            )
        }

        return DataResult.Success(result)
    }

    /**
     * Queries the database based on the specified query builder.
     *
     * @param queryBuilder The query builder to use for the query.
     * @return A [DataResult] containing a list of [Horus.Entity] objects.
     */
    suspend fun query(queryBuilder: QueryBuilder): DataResult<List<Horus.Entity>> {

        return runCatching {

            val entityName = queryBuilder.getTables().first()

            validateConstraintsReadable(entityName)

            val result = operationDatabaseHelper!!.queryRecords(queryBuilder).map {
                Horus.Entity(
                    entityName,
                    it.map { Horus.Attribute(it.key, it.value) }
                )
            }

            return DataResult.Success(result)

        }.getOrElse {
            DataResult.Failure(it)
        }
    }

    /**
     * Retrieves the count of records from entity based on the specified conditions.
     */
    suspend fun countRecordFromEntity(entity: String, vararg whereCondition: SQL.WhereCondition): DataResult<Int> {

        return kotlin.runCatching {

            validateConstraintsReadable(entity)
            val queryBuilder = SimpleQueryBuilder(entity).apply {
                where(*whereCondition)
            }
            return DataResult.Success(operationDatabaseHelper!!.countRecords(queryBuilder))

        }.getOrElse {
            DataResult.Failure(it)
        }
    }

    /**
     * Retrieves the count of records based on the specified query builder.
     */
    suspend fun countRecords(queryBuilder: SimpleQueryBuilder): DataResult<Int> {
        return kotlin.runCatching {
            return DataResult.Success(operationDatabaseHelper!!.countRecords(queryBuilder))
        }.getOrElse {
            DataResult.Failure(it)
        }
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

        return operationDatabaseHelper?.queryRecords(queryBuilder)?.map {
            Horus.Entity(
                entity,
                it.map { Horus.Attribute(it.key, it.value) }
            )
        }?.firstOrNull()
    }

    /**
     * Gets the list of entities available in the database.
     */
    fun getEntityNames(): List<String> {
        return syncControlDatabaseHelper?.getEntityNames() ?: emptyList()
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

                remoteSynchronizatorManager?.trySynchronizeData()
            }
            syncFileUploadedManager?.syncFiles {
                start()
            }
        }
    }

    /**
     * Checks if there are pending actions to synchronize or files to upload.
     *
     * @return `true` if there are pending actions to synchronize, `false` otherwise.
     */
    fun hasDataToSync(): Boolean {
        val hasDataPending = syncControlDatabaseHelper?.getPendingActions()?.isNotEmpty() ?: false
        val hasFilesPending = uploadFileRepository?.hasFilesToUpload() ?: false

        return hasDataPending || hasFilesPending
    }

    /**
     * Gets the last synchronization timestamp.
     *
     * @return The last synchronization timestamp, or `null` if no synchronization has occurred.
     */
    fun getLastSyncDate(): Long? {
        return syncControlDatabaseHelper?.getLastDatetimeCheckpoint()
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
     * Uploads a file to synchronize with the remote server.
     * The synchronization is
     *
     * @param fileData The file data to upload.
     * @return A [Horus.FileReference] object representing the uploaded file.
     */
    fun uploadFile(fileData: FileData): Horus.FileReference {
        validateIsReady()
        return uploadFileRepository!!.createFileLocal(fileData)
    }

    /**
     * Retrieves the URL of an file based on its reference.
     * If the network is not available, the local URL is returned.
     *
     * @param reference The reference of the image.
     * @return The URL of the file if found, `null` otherwise.
     */
    fun getFileUri(reference: CharSequence): String? {

        if (networkValidator?.isNetworkAvailable().isFalse()) {
            return uploadFileRepository?.getFileUrlLocal(reference)
        }

        return uploadFileRepository?.getFileUrl(reference)
    }

    /**
     * Retrieves if horus is ready for operations.
     *
     * @return `true` if horus is ready, `false` otherwise.
     */
    fun isReady(): Boolean {
        return isReady
    }

    // ---------------------------------------------------------------------------------------------
    // Private methods
    // ---------------------------------------------------------------------------------------------

    /**
     * Validates the constraints for the facade.
     */
    private fun validateConstraintsEntity(entity: String) {
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
        syncControlDatabaseHelper?.getEntityNames()?.find { it == entity }
            ?: throw EntityNotExistsException(entity)
    }

    private fun validateIsCanWriteIntoEntity(entity: String) {
        if (syncControlDatabaseHelper!!.isEntityCanBeWritable(entity)) {
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
     * Register Observer Events to do some actions when the event is triggered.
     */
    private fun registerObserverEvents() {
        EventBus.register(EventType.USER_SESSION_CLEARED) {
            syncControlDatabaseHelper?.clearDatabase()
            MemoryCache.flushCache()
        }
    }

    /**
     * Retrieves the authenticated user ID.
     *
     * @return The user ID of the authenticated user.
     * @throws UserNotAuthenticatedException If no user is authenticated.
     */
    private fun getEffectiveUserId(): String {
        return HorusAuthentication.getEffectiveUserId()
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
        operationDatabaseHelper = null
        syncControlDatabaseHelper = null
        remoteSynchronizatorManager = null
        uploadFileRepository = null
    }
}
