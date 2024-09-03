package com.apptank.horus.client.sync.network.dto

import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.data.Horus
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime


/**
 * Checks if a string represents a relation.
 *
 * @receiver The string to check.
 * @return `true` if the string starts with an underscore (`_`), `false` otherwise.
 */
fun String.isRelation() = this.startsWith("_")

/**
 * Converts an [EntityResponse] to [Horus.Entity].
 *
 * Filters the relations and attributes from the entity data and maps them appropriately.
 *
 * @receiver The [EntityResponse] to convert.
 * @return An [Horus.Entity] object containing the processed entity data.
 * @throws IllegalArgumentException if the entity is null.
 */
fun SyncDTO.Response.Entity.toEntityData(): Horus.Entity {

    val relations =
        data?.filter { it.key.startsWith("_") }?.entries?.map {
            it.key to (it.value as ArrayList<LinkedHashMap<String, Any>>).toListEntityResponse()
        }?.associate { it.first.substring(1) to it.second.toListEntityData() }

    return Horus.Entity(
        entity ?: throw IllegalArgumentException(),
        data?.filterNot { it.key.isRelation() }?.mapNotNull { item ->
            item.value?.let { mapAttributeValue(item.key, item.value) }
        } ?: emptyList(),
        relations
    )
}

/**
 * Converts a list of [EntityResponse] objects to a list of [Horus.Entity] objects.
 *
 * @receiver The list of [EntityResponse] objects to convert.
 * @return A list of [Horus.Entity] objects.
 */
fun List<SyncDTO.Response.Entity>.toListEntityData() = this.map { it.toEntityData() }

/**
 * Maps an attribute value to an [Horus.Attribute].
 *
 * Converts the attribute value to the appropriate type.
 *
 * @param name The name of the attribute.
 * @param value The value of the attribute.
 * @return An [Horus.Attribute] containing the name and the processed value.
 * @throws IllegalArgumentException if the attribute value type is not supported.
 */
private fun mapAttributeValue(name: String, value: Any?): Horus.Attribute<*> {

    return Horus.Attribute(
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
 * Converts an [ArrayList] of [LinkedHashMap] objects to a list of [EntityResponse] objects.
 *
 * @receiver The [ArrayList] of [LinkedHashMap] objects to convert.
 * @return A list of [EntityResponse] objects.
 */
private fun ArrayList<LinkedHashMap<String, Any>>.toListEntityResponse(): List<SyncDTO.Response.Entity> {
    return this.map {
        SyncDTO.Response.Entity().apply {
            entity = it["entity"] as? String
            data = (it["data"] as? LinkedHashMap<String, Any?>)?.entries?.associate {
                it.key to it.value
            }
        }
    }
}


/**
 * Extension function to convert a SyncAction to a SyncActionRequest
 */
fun SyncControl.Action.toRequest(): SyncDTO.Request.SyncActionRequest {
    return SyncDTO.Request.SyncActionRequest(
        action = this.action.name,
        entity = this.entity,
        data = this.data,
        datetime = this.datetime.toInstant(TimeZone.UTC).epochSeconds
    )
}

fun SyncDTO.Response.SyncAction.toDomain(): SyncControl.Action {
    return SyncControl.Action(
        id = 0,
        action = SyncControl.ActionType.valueOf(action!!),
        entity = entity ?: throw IllegalArgumentException("Entity is null"),
        status = SyncControl.ActionStatus.COMPLETED,
        data = data ?: mapOf(),
        datetime = actionedAt?.let {
            Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.UTC)
        } ?: throw IllegalArgumentException("DatetimeAction is null")
    )
}

