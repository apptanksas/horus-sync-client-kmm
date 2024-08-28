package com.apptank.horus.client.migration.domain

data class EntityScheme(
    val name: String,
    val attributes: List<Attribute>,
    val currentVersion: Long,
    val entitiesRelated: List<EntityScheme>
)

data class Attribute(
    val name: String,
    val type: AttributeType,
    val isNullable: Boolean,
    val options: List<String> = listOf(),
    val version: Long
)

enum class AttributeType {
    PrimaryKeyInteger,
    PrimaryKeyString,
    PrimaryKeyUUID,
    Integer,
    Float,
    Boolean,
    String,
    Text,
    Json,
    Enum,
    Timestamp,
    UUID,
    RelationOneOfMany,
    RelationOneOfOne
}


/**
 * Search the last version to migrate
 */
fun List<EntityScheme>.getLastVersion(): Long {

    val compareValue = fun(current: Long, new: Long): Long {
        if (new > current) {
            return new
        }
        return current
    }

    var version = 0L

    this.forEach {
        version = compareValue.invoke(version, it.entitiesRelated.getLastVersion())
        it.attributes.forEach {
            version = compareValue.invoke(version, it.version)
        }
    }

    return version
}

fun List<Attribute>.filterRelations(): List<Attribute> {
    return this.filter {
        !listOf(
            AttributeType.RelationOneOfMany,
            AttributeType.RelationOneOfOne
        ).contains(it.type)
    }
}