package com.apptank.horus.client.control


import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.base.DataMap
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.database.Cursor
import com.apptank.horus.client.database.SQLiteHelper
import com.apptank.horus.client.database.builder.SimpleQueryBuilder
import com.apptank.horus.client.database.SQL
import com.apptank.horus.client.extensions.execute
import com.apptank.horus.client.extensions.getRequireInt
import com.apptank.horus.client.extensions.getRequireLong
import com.apptank.horus.client.extensions.handle
import com.apptank.horus.client.eventbus.Event
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Maneja datos asociados al control interno de la sincronización
 */
internal class SyncControlDatabaseHelper(
    databaseName: String,
    driver: SqlDriver,
) : SQLiteHelper(driver, databaseName), ISyncControlDatabaseHelper {

    override fun createControlTables() {
        driver.handle {
            execute(SyncControlTable.SQL_CREATE_TABLE)
            execute(QueueActionsTable.SQL_CREATE_TABLE)
        }
    }

    override fun isStatusCompleted(type: SyncControl.OperationType): Boolean {
        driver.handle {
            return rawQuery(
                "SELECT EXISTS(SELECT 1 FROM ${SyncControlTable.TABLE_NAME} WHERE ${SyncControlTable.ATTR_TYPE} = ${type.id} AND " +
                        "${SyncControlTable.ATTR_STATUS} = ${SyncControl.Status.COMPLETED.id} LIMIT 1)"
            ) { it.getRequireInt(0) == 1 }.any { it }
        }
    }

    override fun getLastDatetimeCheckpoint(): Long {
        driver.handle {
            return rawQuery(
                "SELECT ${SyncControlTable.ATTR_DATETIME} FROM ${SyncControlTable.TABLE_NAME} WHERE ${SyncControlTable.ATTR_TYPE} = ${SyncControl.OperationType.CHECKPOINT.id} ORDER BY ${SyncControlTable.ATTR_ID} DESC LIMIT 1"
            ) { it.getRequireLong(0) }.firstOrNull() ?: 0L
        }
    }

    override fun addSyncTypeStatus(type: SyncControl.OperationType, status: SyncControl.Status) {
        driver.handle {
            insertOrThrow(SyncControlTable.TABLE_NAME, SyncControlTable.mapToCreate(type, status))
        }
    }

    override fun addActionInsert(
        entity: String,
        attributes: List<Horus.Attribute<*>>
    ) {
        validateIfEntityExists(entity)

        addAction(
            entity,
            SyncControl.ActionType.INSERT,
            attributes.associate { it.name to it.value.toString() })
        emitEntityCreated(
            entity,
            attributes.first { it.name == QueueActionsTable.ATTR_ID }.value as String
        )
    }


    /**
     * Add an update action to the queue
     *
     * @param entity Entity name
     * @param id Entity ID
     * @param attributes List of attributes to update
     */
    override fun addActionUpdate(
        entity: String,
        id: Horus.Attribute<String>,
        attributes: List<Horus.Attribute<*>>
    ) {
        validateIfEntityExists(entity)
        addAction(
            entity,
            SyncControl.ActionType.UPDATE,
            mapOf(
                "id" to id.value,
                "attributes" to attributes.associate { it.name to it.value.toString() })
        )
        emitEntityUpdated(entity, id.value)
    }

    /**
     * Add a delete action to the queue
     *
     * @param entity Entity name
     * @param id Entity ID
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
     * Get pending actions from the queue
     *
     * @return List of pending actions
     */
    override fun getPendingActions(): List<SyncControl.Action> {
        driver.handle {
            val sqlSentence = SimpleQueryBuilder(QueueActionsTable.TABLE_NAME)
                .where(
                    SQL.WhereCondition(
                        SQL.ColumnValue(QueueActionsTable.ATTR_STATUS, SyncControl.ActionStatus.PENDING.id)
                    )
                ).orderBy(QueueActionsTable.ATTR_DATETIME).build()

            return queryResult(sqlSentence) { createSyncActionFromCursor(it) }
        }
    }

    /**
     * Update the status of the actions as completed
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
     * Get the last action completed
     *
     * @return Last action completed
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


    override fun getEntityNames(): List<String> {
        return getTablesNames().filterNot { it == SyncControlTable.TABLE_NAME || it == QueueActionsTable.TABLE_NAME }
    }

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

    private fun validateIfEntityExists(entity: String) {
        getEntityNames().find { it == entity } ?: run {
            throw IllegalArgumentException("Entity $entity does not exist")
        }
    }

    private fun emitEventActionCreated() {
        EventBus.post(EventType.ACTION_CREATED, Event())
    }

    private fun emitEntityCreated(entity: String, id: String) {
        EventBus.post(EventType.ENTITY_CREATED, Event(mutableMapOf("entity" to entity, "id" to id)))
    }

    private fun emitEntityUpdated(entity: String, id: String) {
        EventBus.post(EventType.ENTITY_UPDATED, Event(mutableMapOf("entity" to entity, "id" to id)))
    }

    private fun emitEntityDeleted(entity: String, id: String) {
        EventBus.post(EventType.ENTITY_DELETED, Event(mutableMapOf("entity" to entity, "id" to id)))
    }
}