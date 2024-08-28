package com.apptank.horus.client.control


import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.apptank.horus.client.database.DBColumnValue
import com.apptank.horus.client.database.SQLiteHelper
import com.apptank.horus.client.database.WhereCondition
import com.apptank.horus.client.database.builder.SimpleQueryBuilder
import com.apptank.horus.client.domain.EntityAttribute
import com.apptank.horus.client.extensions.execute
import com.apptank.horus.client.extensions.getRequireInt
import com.apptank.horus.client.extensions.getRequireLong
import com.apptank.horus.client.extensions.handle
import com.apptank.horus.client.extensions.rawQuery
import com.apptank.horus.client.eventbus.Event
import com.apptank.horus.client.eventbus.EventBus
import com.apptank.horus.client.eventbus.EventType
import com.apptank.horus.client.extensions.getValue
import com.apptank.horus.client.extensions.insertOrThrow
import com.apptank.horus.client.extensions.getStringAndConvertToMap
import com.apptank.horus.client.extensions.update
import com.apptank.horus.client.utils.SystemTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Maneja datos asociados al control interno de la sincronización
 */
abstract class InternalManageDatabaseHelper(
    databaseName: String,
    driver: SqlDriver,
) : SQLiteHelper(driver, databaseName) {

    fun onCreate() {
        driver.handle {
            execute(SYNC_CONTROL_SQL_CREATE_TABLE)
            execute(QUEUE_ACTIONS_SQL_CREATE_TABLE)
        }
    }

    fun isStatusCompleted(type: SyncOperationType): Boolean {
        driver.handle {
            return rawQuery(
                "SELECT EXISTS(SELECT 1 FROM $SYNC_CONTROL_TABLE_NAME WHERE type = ? LIMIT 1)"
            ) { it.getRequireInt(0) == 1 }.isNotEmpty()
        }
    }

    fun getLastDatetimeCheckpoint(): Long {
        driver.handle {
            return rawQuery(
                "SELECT datetime FROM $SYNC_CONTROL_TABLE_NAME WHERE type = ? ORDER BY id DESC LIMIT 1"
            ) { it.getRequireLong(0) }.firstOrNull() ?: 0L
        }
    }

    fun addSyncTypeStatus(type: SyncOperationType, status: ControlStatus) {
        driver.handle {
            insertOrThrow(
                SYNC_CONTROL_TABLE_NAME, mapOf(
                    "type" to type.id,
                    "status" to status.id,
                    "datetime" to SystemTime.getCurrentTimestamp()
                )
            )
        }
    }

    fun addActionInsert(
        entity: String,
        attributes: List<EntityAttribute<*>>
    ) {

        // TODO("Validate if entity exists")
        // TODO("Validate insertion")
        val dataJSON = Json.encodeToString(attributes.associate { it.name to it.value })
        addAction(entity, SyncActionType.INSERT, dataJSON)
        emitEntityCreated(entity, attributes.first { it.name == "id" }.value as String)
    }


    /**
     * Add an update action to the queue
     *
     * @param entity Entity name
     * @param id Entity ID
     * @param attributes List of attributes to update
     */
    fun addActionUpdate(
        entity: String,
        id: EntityAttribute<String>,
        attributes: List<EntityAttribute<*>>
    ) {
        // TODO("Validate if entity exists")
        val dataJSON = Json.encodeToString(mapOf("id" to id.value, "attributes" to attributes.associate { it.name to it.value }))
        addAction(entity, SyncActionType.UPDATE, dataJSON)
        emitEntityUpdated(entity, id.value)
    }

    /**
     * Add a delete action to the queue
     *
     * @param entity Entity name
     * @param id Entity ID
     */
    fun addActionDelete(
        entity: String,
        id: EntityAttribute<String>
    ) {

        // TODO("Validate if entity exists")
        val dataJSON = Json.encodeToString(mapOf("id" to id.value))
        addAction(entity, SyncActionType.DELETE, dataJSON)
        emitEntityDeleted(entity, id.value)
    }

    /**
     * Update the status of the actions as completed
     */
    fun completeActions(actionIds: List<Int>): Boolean {
        driver.handle {
            val values = mapOf<String, Any>(
                "status" to SyncActionStatus.COMPLETED.id
            )
            val whereClause = "id IN (${actionIds.joinToString(",")})"
            return update(QUEUE_ACTIONS_TABLE_NAME, values, whereClause) == actionIds.size.toLong()
        }
    }

    /**
     * Get pending actions from the queue
     *
     * @return List of pending actions
     */
    fun getPendingActions(): List<SyncAction> {
        driver.handle {
            val sqlSentence = SimpleQueryBuilder(QUEUE_ACTIONS_TABLE_NAME)
                .where(
                    WhereCondition(
                        DBColumnValue("status", SyncActionStatus.PENDING.id),
                        "="
                    )
                ).orderBy("datetime").build()

            return rawQuery(sqlSentence) { createSyncActionFromCursor(it) }
        }
    }

    /**
     * Get the last action completed
     *
     * @return Last action completed
     */
    fun getLastActionCompleted(): SyncAction? {
        driver.handle {
            val sentenceSql = SimpleQueryBuilder(QUEUE_ACTIONS_TABLE_NAME)
                .where(
                    WhereCondition(
                        DBColumnValue("status", SyncActionStatus.COMPLETED.id),
                        "="
                    )
                ).orderBy("id").limit(1).build()

            return rawQuery(sentenceSql) { createSyncActionFromCursor(it) }.firstOrNull()
        }
    }


    fun getCompletedActionsAfterDatetime(datetime: Long): List<SyncAction> {
        driver.handle {
            val sqlSentence = SimpleQueryBuilder(QUEUE_ACTIONS_TABLE_NAME)
                .where(
                    WhereCondition(
                        DBColumnValue("datetime", datetime), ">"
                    )
                )
                .where(
                    WhereCondition(
                        DBColumnValue("status", SyncActionStatus.COMPLETED.id),
                        "="
                    )
                ).orderBy("datetime").build()

            return rawQuery(sqlSentence) { createSyncActionFromCursor(it) }
        }
    }


    fun getEntityNames(): List<String> {
        return getTablesNames().filterNot { it == SYNC_CONTROL_TABLE_NAME || it == QUEUE_ACTIONS_TABLE_NAME }
    }


    private fun createSyncActionFromCursor(cursor: SqlCursor): SyncAction {
        return SyncAction(
            cursor.getValue("id"),
            SyncActionType.fromId(cursor.getValue<Int>("action_type")),
            cursor.getValue<String>("entity"),
            SyncActionStatus.fromId(cursor.getValue<Int>("status")),
            cursor.getStringAndConvertToMap("data"),
            Instant.fromEpochSeconds(cursor.getValue<Long>("datetime"))
                .toLocalDateTime(TimeZone.UTC)
        )
    }

    private fun addAction(
        entity: String,
        actionType: SyncActionType,
        dataJSON: String
    ) {
        driver.handle {
            insertOrThrow(
                QUEUE_ACTIONS_TABLE_NAME, mapOf(
                    "action_type" to actionType.id,
                    "entity" to entity,
                    "data" to dataJSON,
                    "status" to SyncActionStatus.PENDING.id,
                    "datetime" to SystemTime.getCurrentTimestamp()
                )
            )
        }
        emitEventActionCreated()
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

    private companion object {
        const val SYNC_CONTROL_TABLE_NAME = "sync_control"
        const val SYNC_CONTROL_SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $SYNC_CONTROL_TABLE_NAME (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "type INTEGER," +
                    "status INTEGER," +
                    "datetime INTEGER)"
        const val QUEUE_ACTIONS_TABLE_NAME = "queue_actions"
        const val QUEUE_ACTIONS_SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS $QUEUE_ACTIONS_TABLE_NAME (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "action_type INTEGER," +
                    "entity TEXT," +
                    "data TEXT," +
                    "status INTEGER," +
                    "datetime INTEGER)"
    }
}