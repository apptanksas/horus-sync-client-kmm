package com.apptank.horus.client.data

import com.apptank.horus.client.database.LocalDatabase
import com.apptank.horus.client.database.toDBColumnValue
import com.apptank.horus.client.extensions.forEachPair

data class EntityDataRelations(
    val name: String,
    val attributes: List<EntityAttribute<*>>,
    val relations: Map<String, List<EntityDataRelations>>? = null
)

data class EntityAttribute<T>(
    val name: String,
    val value: T
)

fun EntityDataRelations.toRecordsInsert(): List<LocalDatabase.InsertRecord> {

    val records = mutableListOf<LocalDatabase.InsertRecord>()

    this.relations?.forEachPair { relation, entities ->
        entities.forEach {
            records.addAll(it.toRecordsInsert())
        }
    }

    records.add(LocalDatabase.InsertRecord(
        this.name, this.attributes.map {
            it.toDBColumnValue()
        }
    ))
    return records
}





