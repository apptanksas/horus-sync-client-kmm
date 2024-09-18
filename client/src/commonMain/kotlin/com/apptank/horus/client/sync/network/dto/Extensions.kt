package com.apptank.horus.client.sync.network.dto

import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.data.InternalModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime


/**
 * Extension function to determine if the string represents a relation.
 *
 * @return True if the string starts with an underscore ("_"), otherwise false.
 */
fun String.isRelation() = this.startsWith("_")

/**
 * Converts a [SyncDTO.Response.Entity] to a [Horus.Entity].
 *
 * @return The [Horus.Entity] representation of the response entity.
 *
 * @throws IllegalArgumentException if the entity name is null.
 */
fun SyncDTO.Response.Entity.toEntityData(): Horus.Entity {

    val relations =
        data?.filter { it.key.startsWith("_") }?.entries?.map {
            it.key to (it.value as ArrayList<LinkedHashMap<String, Any>>).toListEntityResponse()
        }?.associate { it.first.substring(1) to it.second.toListEntityData() }

    return Horus.Entity(
        entity ?: throw IllegalArgumentException("Entity name is null"),
        data?.filterNot { it.key.isRelation() }?.mapNotNull { item ->
            item.value?.let { mapAttributeValue(item.key, item.value) }
        } ?: emptyList(),
        relations
    )
}

/**
 * Converts a list of [SyncDTO.Response.Entity] to a list of [Horus.Entity].
 *
 * @return The list of [Horus.Entity] representations.
 */
fun List<SyncDTO.Response.Entity>.toListEntityData() = this.map { it.toEntityData() }

/**
 * Maps attribute name and value to a [Horus.Attribute].
 *
 * @param name The name of the attribute.
 * @param value The value of the attribute.
 * @return The [Horus.Attribute] corresponding to the given name and value.
 *
 * @throws IllegalArgumentException if the value type is not supported.
 */
private fun mapAttributeValue(name: String, value: Any?): Horus.Attribute<*> {

    return Horus.Attribute(
        name,
        when (value) {
            is Int, Float -> value
            is Double -> value
            is String -> value
            is Long -> value
            else -> throw IllegalArgumentException("Attribute value [" + if (value == null) "" else value::class.simpleName + "] not supported")
        }
    )
}

/**
 * Converts an [ArrayList] of [LinkedHashMap] representing entities to a list of [SyncDTO.Response.Entity].
 *
 * @return The list of [SyncDTO.Response.Entity] representations.
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
 * Converts a [SyncControl.Action] to a [SyncDTO.Request.SyncActionRequest].
 *
 * @return The [SyncDTO.Request.SyncActionRequest] representation of the action.
 */
internal fun SyncControl.Action.toRequest(): SyncDTO.Request.SyncActionRequest {
    return SyncDTO.Request.SyncActionRequest(
        action = this.action.name,
        entity = this.entity,
        data = this.data,
        actionedAt = this.actionedAt.toInstant(TimeZone.UTC).epochSeconds
    )
}

/**
 * Converts a [SyncDTO.Response.SyncAction] to a [SyncControl.Action].
 *
 * @return The [SyncControl.Action] representation of the sync action response.
 *
 * @throws IllegalArgumentException if the entity or action timestamp is null.
 */
internal fun SyncDTO.Response.SyncAction.toDomain(): SyncControl.Action {
    return SyncControl.Action(
        id = 0,
        action = SyncControl.ActionType.valueOf(action!!),
        entity = entity ?: throw IllegalArgumentException("Entity is null"),
        status = SyncControl.ActionStatus.COMPLETED,
        data = data ?: mapOf(),
        actionedAt = actionedAt?.let {
            Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.UTC)
        } ?: throw IllegalArgumentException("DatetimeAction is null")
    )
}

/**
 * Converts a [SyncDTO.Response.EntityIdHash] to an [InternalModel.EntityIdHash].
 *
 * @return The [InternalModel.EntityIdHash] representation.
 *
 * @throws IllegalArgumentException if the ID or hash is null.
 */
internal fun SyncDTO.Response.EntityIdHash.toInternalModel(): InternalModel.EntityIdHash {
    return InternalModel.EntityIdHash(
        id ?: throw IllegalArgumentException("Entity ID is null"),
        hash ?: throw IllegalArgumentException("Hash is null")
    )
}

/**
 * Converts a [SyncDTO.Response.EntityHash] to an [InternalModel.EntityHashValidation].
 *
 * @return The [InternalModel.EntityHashValidation] representation.
 *
 * @throws IllegalArgumentException if the entity or hash values are null.
 */
internal fun SyncDTO.Response.EntityHash.toInternalModel(): InternalModel.EntityHashValidation {
    return InternalModel.EntityHashValidation(
        entity ?: throw IllegalArgumentException("Entity is null"),
        hashingValidation?.expected ?: throw IllegalArgumentException("Expected hash is null"),
        hashingValidation?.obtained ?: throw IllegalArgumentException("Obtained hash is null"),
        hashingValidation?.matched ?: throw IllegalArgumentException("Matched status is null")
    )
}
