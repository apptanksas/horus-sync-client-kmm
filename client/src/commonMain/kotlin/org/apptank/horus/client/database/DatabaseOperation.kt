package org.apptank.horus.client.database

import org.apptank.horus.client.control.SyncControl
import org.apptank.horus.client.data.Horus
import org.apptank.horus.client.extensions.forEachPair
import org.apptank.horus.client.extensions.removeIf
import org.apptank.horus.client.utils.AttributesPreparator

/**
 * Represents a database operation for different CRUD actions.
 *
 * @property table The name of the table where the operation will be applied.
 */
sealed class DatabaseOperation(open val table: String) {

    /**
     * Represents an insert operation for a record in a database table.
     *
     * @property table The name of the table where the record will be inserted.
     * @property values A list of column values to be inserted.
     */
    data class InsertRecord(
        override val table: String,
        val values: List<SQL.ColumnValue>
    ) : DatabaseOperation(table)

    /**
     * Represents an update operation for a record in a database table.
     *
     * @property table The name of the table where the record will be updated.
     * @property values A list of column values to be updated.
     * @property conditions A list of conditions that specify which records to update.
     * @property operator The logical operator used to combine conditions. Default is [SQL.LogicOperator.AND].
     */
    data class UpdateRecord(
        override val table: String,
        val values: List<SQL.ColumnValue>,
        val conditions: List<SQL.WhereCondition>,
        val operator: SQL.LogicOperator = SQL.LogicOperator.AND
    ) : DatabaseOperation(table)

    /**
     * Represents a delete operation for a record in a database table.
     *
     * @property table The name of the table where the record will be deleted.
     * @property conditions A list of conditions that specify which records to delete.
     * @property operator The logical operator used to combine conditions. Default is [SQL.LogicOperator.AND].
     */
    data class DeleteRecord(
        override val table: String,
        val conditions: List<SQL.WhereCondition>,
        val operator: SQL.LogicOperator = SQL.LogicOperator.AND
    ) : DatabaseOperation(table)

    /**
     * Data class representing the result of a database operation.
     *
     * @property isSuccess Indicates if the operation was successful.
     * @property rowsAffected The number of rows affected by the operation.
     * @property isFailure Indicates if the operation was failure.
     */
    data class Result(
        val isSuccess: Boolean,
        val rowsAffected: Int,
        val isFailure: Boolean = !isSuccess,
    )
}

/**
 * Converts the [Horus.Entity] to a list of [DatabaseOperation.InsertRecord] objects.
 *
 * @return A list of [DatabaseOperation.InsertRecord] objects representing the entity's data to be inserted.
 */
internal fun Horus.Entity.toRecordsInsert(): List<DatabaseOperation.InsertRecord> {

    val records = mutableListOf<DatabaseOperation.InsertRecord>()

    // Recursively add insert records for related entities
    this.relations?.forEachPair { relation, entities ->
        entities.forEach {
            records.addAll(it.toRecordsInsert())
        }
    }

    // Add the current entity's insert record
    records.add(
        DatabaseOperation.InsertRecord(
            this.name, this.attributes.map {
                it.toDBColumnValue()
            }
        ))
    return records
}

/**
 * Converts a [SyncControl.Action] to a [DatabaseOperation.InsertRecord].
 *
 * @param userId The user ID associated with the action.
 * @return A [DatabaseOperation.InsertRecord] representing the insert action.
 * @throws IllegalArgumentException If the action type is not [SyncControl.ActionType.INSERT].
 */
internal fun SyncControl.Action.toInsertRecord(userId: String): DatabaseOperation.InsertRecord {

    if (action != SyncControl.ActionType.INSERT) {
        throw IllegalArgumentException("Action type must be INSERT")
    }

    val id = Horus.Attribute(Horus.Attribute.ID, getEntityId())
    val attributes = data
        .filterNot { it.key == Horus.Attribute.ID }
        .map { Horus.Attribute(it.key, it.value) }
        .toList()

    val attributesPrepared = AttributesPreparator.appendHashAndUpdateAttributes(
        id,
        AttributesPreparator.appendInsertSyncAttributes(
            id,
            attributes,
            userId,
            getActionedAtTimestamp()
        ), getActionedAtTimestamp()
    )
    return DatabaseOperation.InsertRecord(
        entity,
        attributesPrepared.map { it.toDBColumnValue() }
    )
}

/**
 * Converts a [SyncControl.Action] to a [DatabaseOperation.UpdateRecord].
 *
 * @param currentEntity The current state of the entity to be updated.
 * @return A [DatabaseOperation.UpdateRecord] representing the update action.
 */
internal fun SyncControl.Action.toUpdateRecord(currentEntity: Horus.Entity): DatabaseOperation.UpdateRecord {

    val id = getEntityId()
    val attributes: List<Horus.Attribute<*>> = (getEntityAttributes()).map {
        Horus.Attribute(it.key, it.value)
    }.toList()

    val attrId = Horus.Attribute("id", id)

    val attributesPrepared = AttributesPreparator.appendHashAndUpdateAttributes(
        attrId,
        attributes.toMutableList().apply {
            currentEntity.attributes.filter { currentAttribute ->
                attributes.find { it.name == currentAttribute.name } == null
            }.forEach {
                add(it)
            }
            removeIf { it.name == Horus.Attribute.HASH }
        })

    return DatabaseOperation.UpdateRecord(
        entity, attributesPrepared.mapToDBColumValue(),
        listOf(SQL.WhereCondition(SQL.ColumnValue("id", id)))
    )
}

/**
 * Converts a [SyncControl.Action] to a [DatabaseOperation.DeleteRecord].
 *
 * @return A [DatabaseOperation.DeleteRecord] representing the delete action.
 */
internal fun SyncControl.Action.toDeleteRecord(): DatabaseOperation.DeleteRecord {
    return DatabaseOperation.DeleteRecord(
        entity,
        listOf(SQL.WhereCondition(SQL.ColumnValue(Horus.Attribute.ID, getEntityId())))
    )
}
