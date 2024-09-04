package com.apptank.horus.client.database

import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.extensions.forEachPair
import com.apptank.horus.client.extensions.log
import com.apptank.horus.client.extensions.removeIf
import com.apptank.horus.client.utils.AttributesPreparator


/**
 * Base class representing an action to be performed on a database table.
 *
 * @property table The name of the database table.
 */

sealed class DatabaseOperation(open val table: String) {

    /**
     * Data class representing an insert action to be performed on a database table.
     *
     * @property table The name of the database table.
     * @property values A list of column-value pairs to be inserted.
     */
    data class InsertRecord(
        override val table: String,
        val values: List<SQL.ColumnValue>
    ) : DatabaseOperation(table)

    /**
     * Data class representing an update operation on a database record.
     *
     * @property table The name of the table.
     * @property values The list of column-value pairs to be updated.
     * @property conditions The list of conditions for the update.
     * @property operator The logical operator to combine conditions (AND/OR).
     */
    data class UpdateRecord(
        override val table: String,
        val values: List<SQL.ColumnValue>,
        val conditions: List<SQL.WhereCondition>,
        val operator: SQL.LogicOperator = SQL.LogicOperator.AND
    ) : DatabaseOperation(table)


    /**
     * Data class representing a delete action to be performed on a database table.
     *
     * @property table The name of the database table.
     * @property values A list of column-value pairs to be used as conditions for deletion.
     */
    data class DeleteRecord(
        override val table: String,
        val conditions: List<SQL.WhereCondition>,
        val operator: SQL.LogicOperator = SQL.LogicOperator.AND
    ) : DatabaseOperation(table)
}


fun Horus.Entity.toRecordsInsert(): List<DatabaseOperation.InsertRecord> {

    val records = mutableListOf<DatabaseOperation.InsertRecord>()

    this.relations?.forEachPair { relation, entities ->
        entities.forEach {
            records.addAll(it.toRecordsInsert())
        }
    }
    records.add(
        DatabaseOperation.InsertRecord(
            this.name, this.attributes.map {
                it.toDBColumnValue()
            }
        ))
    return records
}


fun SyncControl.Action.toInsertRecord(userId: String): DatabaseOperation.InsertRecord {

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

fun SyncControl.Action.toUpdateRecord(currentEntity: Horus.Entity): DatabaseOperation.UpdateRecord {

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

fun SyncControl.Action.toDeleteRecord(): DatabaseOperation.DeleteRecord {
    return DatabaseOperation.DeleteRecord(
        entity,
        listOf(SQL.WhereCondition(SQL.ColumnValue(Horus.Attribute.ID, getEntityId())))
    )
}