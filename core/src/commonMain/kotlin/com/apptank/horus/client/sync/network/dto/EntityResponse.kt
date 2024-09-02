package com.apptank.horus.client.sync.network.dto


import com.apptank.horus.client.base.DataMap
import com.apptank.horus.client.domain.EntityAttribute
import com.apptank.horus.client.domain.EntityDataRelations
import kotlinx.serialization.Serializable

/**
 * Represents the response from an entity API.
 *
 * @property entity The name of the entity.
 * @property data The data associated with the entity, stored as a map of attributes.
 */
@Serializable
data class EntityResponse(
    var entity: String? = null,
    var data: DataMap? = null
)

/**
 * Checks if a string represents a relation.
 *
 * @receiver The string to check.
 * @return `true` if the string starts with an underscore (`_`), `false` otherwise.
 */
fun String.isRelation() = this.startsWith("_")

/**
 * Converts an [EntityResponse] to [EntityDataRelations].
 *
 * Filters the relations and attributes from the entity data and maps them appropriately.
 *
 * @receiver The [EntityResponse] to convert.
 * @return An [EntityDataRelations] object containing the processed entity data.
 * @throws IllegalArgumentException if the entity is null.
 */
fun EntityResponse.toEntityData(): EntityDataRelations {

    val relations =
        data?.filter { it.key.startsWith("_") }?.entries?.map {
            it.key to (it.value as ArrayList<LinkedHashMap<String, Any>>).toListEntityResponse()
        }?.associate { it.first.substring(1) to it.second.toListEntityData() }

    return EntityDataRelations(
        entity ?: throw IllegalArgumentException(),
        data?.filterNot { it.key.isRelation() }?.mapNotNull { item ->
            item.value?.let { mapAttributeValue(item.key, item.value) }
        } ?: emptyList(),
        relations
    )
}

/**
 * Converts a list of [EntityResponse] objects to a list of [EntityDataRelations] objects.
 *
 * @receiver The list of [EntityResponse] objects to convert.
 * @return A list of [EntityDataRelations] objects.
 */
fun List<EntityResponse>.toListEntityData() = this.map { it.toEntityData() }

/**
 * Maps an attribute value to an [EntityAttribute].
 *
 * Converts the attribute value to the appropriate type.
 *
 * @param name The name of the attribute.
 * @param value The value of the attribute.
 * @return An [EntityAttribute] containing the name and the processed value.
 * @throws IllegalArgumentException if the attribute value type is not supported.
 */
private fun mapAttributeValue(name: String, value: Any?): EntityAttribute<*> {

    return EntityAttribute(
        name,
        when (value) {
            is Int, Float -> value
            is Double -> value
            is String -> value
            else -> throw IllegalArgumentException("Attribute value [" + if (value == null) "" else value::class.simpleName + "] not supported")
        }
    )
}

/**
 * Converts an [ArrayList] of [LinkedTreeMap] objects to a list of [EntityResponse] objects.
 *
 * @receiver The [ArrayList] of [LinkedTreeMap] objects to convert.
 * @return A list of [EntityResponse] objects.
 */
private fun ArrayList<LinkedHashMap<String, Any>>.toListEntityResponse(): List<EntityResponse> {
    return this.map {
        EntityResponse().apply {
            entity = it["entity"] as? String
            data = (it["data"] as? LinkedHashMap<String, Any?>)?.entries?.associate {
                it.key to it.value
            }
        }
    }
}