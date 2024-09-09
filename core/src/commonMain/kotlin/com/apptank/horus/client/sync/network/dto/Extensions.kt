package com.apptank.horus.client.sync.network.dto

import com.apptank.horus.client.control.SyncControl
import com.apptank.horus.client.data.Horus
import com.apptank.horus.client.data.InternalModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime


fun String.isRelation() = this.startsWith("_")

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

fun List<SyncDTO.Response.Entity>.toListEntityData() = this.map { it.toEntityData() }

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


fun SyncControl.Action.toRequest(): SyncDTO.Request.SyncActionRequest {
    return SyncDTO.Request.SyncActionRequest(
        action = this.action.name,
        entity = this.entity,
        data = this.data,
        datetime = this.actionedAt.toInstant(TimeZone.UTC).epochSeconds
    )
}

fun SyncDTO.Response.SyncAction.toDomain(): SyncControl.Action {
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


fun SyncDTO.Response.EntityIdHash.toInternalModel(): InternalModel.EntityIdHash {
    return InternalModel.EntityIdHash(
        id ?: throw IllegalArgumentException("Entity is null"),
        hash ?: throw IllegalArgumentException("Hash is null")
    )
}

fun SyncDTO.Response.EntityHash.toInternalModel(): InternalModel.EntityHashValidation {
    return InternalModel.EntityHashValidation(
        entity ?: throw IllegalArgumentException("Entity is null"),
        hashingValidation?.expected ?: throw IllegalArgumentException("Hash is null"),
        hashingValidation?.obtained ?: throw IllegalArgumentException("Hash is null"),
        hashingValidation?.matched ?: throw IllegalArgumentException("Matched is null")
    )
}