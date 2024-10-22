package org.apptank.horus.client.database


import app.cash.sqldelight.db.SqlDriver
import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.database.builder.SimpleQueryBuilder
import org.apptank.horus.client.extensions.getRequireInt
import org.apptank.horus.client.extensions.getRequireLong
import org.apptank.horus.client.extensions.handle
import org.apptank.horus.client.eventbus.Event
import org.apptank.horus.client.eventbus.EventBus
import org.apptank.horus.client.eventbus.EventType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.apptank.horus.client.control.QueueActionsTable
import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.control.helper.ISyncControlDatabaseHelper
import org.apptank.horus.client.control.scheme.SyncControlTable
import org.apptank.horus.client.database.struct.Cursor
import org.apptank.horus.client.database.struct.SQL
import org.apptank.horus.client.migration.domain.AttributeType

/**
 * Implementation of the `ISyncControlDatabaseHelper` interface for managing synchronization control data
 * in an SQLite database. Provides methods for checking statuses, managing synchronization types, and handling
 * actions such as inserts, updates, and deletions.
 *
 * @param databaseName The name of the SQLite database.
 * @param driver The SQL driver used for database operations.
 *
 * @author John Ospina
 * @year 2024
 */
internal class SyncControlDatabaseHelper(
    databaseName: String,
    driver: SqlDriver,
) : SQLiteHelper(driver, databaseName), ISyncControlDatabaseHelper {

    /**
     * Checks if the status for a given operation type is completed.
     *
     * @param type The type of synchronization operation.
     * @return True if the status is completed, false otherwise.
     */
    override fun isStatusCompleted(type: SyncControl.OperationType): Boolean {
        driver.handle {
            return rawQuery(
                "SELECT EXISTS(SELECT 1 FROM ${SyncControlTable.TABLE_NAME} WHERE ${SyncControlTable.ATTR_TYPE} = ${type.id} AND " +
                        "${SyncControlTable.ATTR_STATUS} = ${SyncControl.Status.COMPLETED.id} LIMIT 1)"
            ) { it.getRequireInt(0) == 1 }.any { it }
        }
    }

    /**
     * Retrieves the timestamp of the last datetime checkpoint.
     *
     * @return The timestamp of the last checkpoint in milliseconds.
     */
    override fun getLastDatetimeCheckpoint(): Long {
        driver.handle {
            val query = SimpleQueryBuilder(SyncControlTable.TABLE_NAME).apply {
                select(SyncControlTable.ATTR_DATETIME)
                where(
                    SQL.WhereCondition(
                        SQL.ColumnValue(
                            SyncControlTable.ATTR_TYPE,
                            SyncControl.OperationType.CHECKPOINT.id
                        )
                    )
                )
                where(
                    SQL.WhereCondition(
                        SQL.ColumnValue(
                            SyncControlTable.ATTR_STATUS,
                            SyncControl.Status.COMPLETED.id
                        )
                    )
                )
                orderBy(SyncControlTable.ATTR_ID, SQL.OrderBy.DESC)
                limit(1)
            }
            return rawQuery(query.build()) { it.getRequireLong(0) }.firstOrNull() ?: 0L
        }
    }

    /**
     * Adds a new synchronization type status to the database.
     *
     * @param type The type of synchronization operation.
     * @param status The status to be recorded.
     */
    override fun addSyncTypeStatus(type: SyncControl.OperationType, status: SyncControl.Status) {
        driver.handle {
            insertOrThrow(SyncControlTable.TABLE_NAME, SyncControlTable.mapToCreate(type, status))
        }
    }

    /**
     * Inserts a new action for an entity into the database.
     *
     * @param entity The name of the entity.
     * @param attributes The attributes of the entity to be inserted.
     */
    override fun addActionInsert(
        entity: String,
        attributes: List<Horus.Attribute<*>>
    ) {
        validateIfEntityExists(entity)

        val data = attributes.associate { it.name to it.value }

        addAction(
            entity,
            SyncControl.ActionType.INSERT,
            data
        )

        emitEntityCreated(
            entity,
            attributes.first { it.name == QueueActionsTable.ATTR_ID }.value as String,
            data
        )
    }

    /**
     * Adds an update action to the queue.
     *
     * @param entity The name of the entity.
     * @param id The identifier of the entity.
     * @param attributes The attributes to be updated.
     */
    override fun addActionUpdate(
        entity: String,
        id: Horus.Attribute<String>,
        attributes: List<Horus.Attribute<*>>
    ) {
        validateIfEntityExists(entity)

        val data: DataMap = attributes.associate { it.name to it.value }

        addAction(
            entity,
            SyncControl.ActionType.UPDATE,
            mapOf(
                "id" to id.value,
                "attributes" to data
            )
        )
        emitEntityUpdated(entity, id.value, data)
    }

    /**
     * Deletes an action for an entity from the database.
     *
     * @param entity The name of the entity.
     * @param id The identifier of the entity.
     */
    override fun addActionDelete(
        entity: String,
        id: Horus.Attribute<String>
    ) {
        validateIfEntityExists(entity)
        addAction(entity, SyncControl.ActionType.DELETE, mapOf("id" to id.value))
        emitEntityDeleted(entity, id.value)
    }

    /**
     * Retrieves a list of pending actions from the database.
     *
     * @return A list of pending synchronization actions.
     */
    override fun getPendingActions(): List<SyncControl.Action> {
        driver.handle {
            val sqlSentence = SimpleQueryBuilder(QueueActionsTable.TABLE_NAME)
                .where(
                    SQL.WhereCondition(
                        SQL.ColumnValue(
                            QueueActionsTable.ATTR_STATUS,
                            SyncControl.ActionStatus.PENDING.id
                        )
                    )
                ).orderBy(QueueActionsTable.ATTR_DATETIME).build()

            return queryResult(sqlSentence) { createSyncActionFromCursor(it) }
        }
    }

    /**
     * Marks specified actions as completed.
     *
     * @param actionIds The identifiers of the actions to be marked as completed.
     * @return True if the actions were successfully marked as completed, false otherwise.
     */
    override fun completeActions(actionIds: List<Int>): Boolean {
        driver.handle {
            val values = mapOf<String, Any>(
                QueueActionsTable.ATTR_STATUS to SyncControl.ActionStatus.COMPLETED.id
            )
            val whereClause = "${QueueActionsTable.ATTR_ID} IN (${actionIds.joinToString(",")})"
            return update(
                QueueActionsTable.TABLE_NAME,
                values,
                whereClause
            ) == actionIds.size.toLong()
        }
    }

    /**
     * Retrieves the last completed action from the database.
     *
     * @return The last completed synchronization action, or null if no actions have been completed.
     */
    override fun getLastActionCompleted(): SyncControl.Action? {
        driver.handle {
            val sentenceSql = SimpleQueryBuilder(QueueActionsTable.TABLE_NAME)
                .where(
                    SQL.WhereCondition(
                        SQL.ColumnValue(
                            QueueActionsTable.ATTR_STATUS,
                            SyncControl.ActionStatus.COMPLETED.id
                        )
                    )
                ).orderBy(QueueActionsTable.ATTR_ID).limit(1).build()

            return queryResult(sentenceSql) { createSyncActionFromCursor(it) }.firstOrNull()
        }
    }

    /**
     * Retrieves all completed actions that occurred after a specified datetime.
     *
     * @param datetime The timestamp to filter completed actions.
     * @return A list of completed synchronization actions that occurred after the specified datetime.
     */
    override fun getCompletedActionsAfterDatetime(datetime: Long): List<SyncControl.Action> {
        driver.handle {
            val sqlSentence = SimpleQueryBuilder(QueueActionsTable.TABLE_NAME)
                .where(
                    SQL.WhereCondition(
                        SQL.ColumnValue(QueueActionsTable.ATTR_DATETIME, datetime),
                        SQL.Comparator.GREATER_THAN
                    )
                )
                .where(
                    SQL.WhereCondition(
                        SQL.ColumnValue(
                            QueueActionsTable.ATTR_STATUS,
                            SyncControl.ActionStatus.COMPLETED.id
                        )
                    )
                ).orderBy(QueueActionsTable.ATTR_DATETIME).build()

            return queryResult(sqlSentence) { createSyncActionFromCursor(it) }
        }
    }

    /**
     * Retrieves a list of all entity names from the database, excluding sync control and queue actions tables.
     *
     * @return A list of entity names.
     */
    override fun getEntityNames(): List<String> {
        return getTableEntities().map { it.name }
            .filterNot { it == SyncControlTable.TABLE_NAME || it == QueueActionsTable.TABLE_NAME }
    }

    /**
     * Retrieves a list of all entity names from the database that can be written to.
     *
     * @return A list of entity names.
     */
    override fun getWritableEntityNames(): List<String> {
        return getTableEntities().filter { it.isWritable }.map { it.name }
            .filterNot { it == SyncControlTable.TABLE_NAME || it == QueueActionsTable.TABLE_NAME }
    }

    /**
     * Retrieves a list of all entity names that have a specified attribute type.
     *
     * @param type The attribute type to filter entities by.
     * @return A list of entity names.
     */
    override fun getEntitiesWithAttributeType(type: AttributeType): List<String> {
        val entities = getEntityNames()
        val output = mutableListOf<String>()

        for (entity in entities) {
            val columns = getColumns(entity)
            for (column in columns) {
                if (column.format == type) {
                    output.add(entity)
                    break
                }
            }
        }
        return output
    }

    /**
     * Retrieves a list of all attribute names from entity that have a specified attribute type.
     *
     * @param entityName The name of the entity to filter attributes by.
     * @param type The attribute type to filter attributes by.
     * @return A list of entity names.
     */
    override fun getEntityAttributesWithType(
        entityName: String,
        type: AttributeType
    ): List<String> {
        return getColumns(entityName).filter { it.format == type }.map { it.name }
    }

    /**
     * Checks if an entity can be written to.
     *
     * @param entityName The name of the entity to check.
     * @return True if the entity can be written to, false otherwise.
     */
    override fun isEntityCanBeWritable(entityName: String): Boolean {
        return getTableEntities().filter { it.isWritable }.any { it.name == entityName }
    }

    /**
     * Creates a `SyncControl.Action` object from a database cursor.
     *
     * @param cursor The cursor containing the data.
     * @return The `SyncControl.Action` object.
     */
    private fun createSyncActionFromCursor(cursor: Cursor): SyncControl.Action {
        return SyncControl.Action(
            cursor.getValue("id"),
            SyncControl.ActionType.fromId(cursor.getValue("action_type")),
            cursor.getValue("entity"),
            SyncControl.ActionStatus.fromId(cursor.getValue("status")),
            cursor.getStringAndConvertToMap("data"),
            Instant.fromEpochSeconds(cursor.getValue("datetime"))
                .toLocalDateTime(TimeZone.UTC)
        )
    }

    /**
     * Adds a new action to the queue.
     *
     * @param entity The name of the entity.
     * @param actionType The type of action to be performed.
     * @param dataJSON The data associated with the action.
     */
    private fun addAction(
        entity: String,
        actionType: SyncControl.ActionType,
        dataJSON: DataMap
    ) {
        driver.handle {
            insertOrThrow(
                QueueActionsTable.TABLE_NAME,
                QueueActionsTable.mapToCreate(actionType, entity, dataJSON)
            )
        }
        emitEventActionCreated()
    }

    /**
     * Validates if the specified entity exists in the database.
     *
     * @param entity The name of the entity to be validated.
     * @throws IllegalArgumentException If the entity does not exist.
     */
    private fun validateIfEntityExists(entity: String) {
        getEntityNames().find { it == entity } ?: run {
            throw IllegalArgumentException("Entity $entity does not exist")
        }
    }

    /**
     * Emits an event indicating that an action has been created.
     */
    private fun emitEventActionCreated() {
        EventBus.emit(EventType.ACTION_CREATED, Event())
    }

    /**
     * Emits an event indicating that an entity has been created.
     *
     * @param entity The name of the entity.
     * @param id The identifier of the entity.
     * @param data The attributes of the entity.
     */
    private fun emitEntityCreated(entity: String, id: String, data: DataMap) {
        EventBus.emit(
            EventType.ENTITY_CREATED,
            Event(mutableMapOf("entity" to entity, "id" to id, "attributes" to data))
        )
    }

    /**
     * Emits an event indicating that an entity has been updated.
     *
     * @param entity The name of the entity.
     * @param id The identifier of the entity.
     * @param data The updated attributes of the entity.
     */
    private fun emitEntityUpdated(entity: String, id: String, data: DataMap) {
        EventBus.emit(
            EventType.ENTITY_UPDATED,
            Event(mutableMapOf("entity" to entity, "id" to id, "attributes" to data))
        )
    }

    /**
     * Emits an event indicating that an entity has been deleted.
     *
     * @param entity The name of the entity.
     * @param id The identifier of the entity.
     */
    private fun emitEntityDeleted(entity: String, id: String) {
        EventBus.emit(EventType.ENTITY_DELETED, Event(mutableMapOf("entity" to entity, "id" to id)))
    }
}
