package com.apptank.horus.client.sync.network.dto

import com.apptank.horus.client.base.MapAttributes
import com.apptank.horus.client.control.SyncAction
import com.apptank.horus.client.control.SyncActionStatus
import com.apptank.horus.client.control.SyncActionType
import com.apptank.horus.client.serialization.AnySerializer
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable


/**
 * Request model for a sync action
 */
data class SyncActionRequest(
    private val action: String,
    private val entity: String,
    private val data: MapAttributes,
    private val datetime: Long
)

/**
 * Extension function to convert a SyncAction to a SyncActionRequest
 */
fun SyncAction.toRequest(): SyncActionRequest {
    return SyncActionRequest(
        action = this.action.name,
        entity = this.entity,
        data = this.data,
        datetime = this.datetime.toInstant(TimeZone.UTC).epochSeconds
    )
}


@Serializable
data class SyncActionResponse(
    val action: String? = null,
    val entity: String? = null,
    val data: Map<String, @Serializable(with = AnySerializer::class) Any?>? = null,
    val datetimeAction: Int? = null,
    val datetimeSync: Int? = null
)

fun SyncActionResponse.toDomain(): SyncAction {
    return SyncAction(
        id = 0,
        action = SyncActionType.valueOf(action!!),
        entity = entity ?: throw IllegalArgumentException("Entity is null"),
        status = SyncActionStatus.COMPLETED,
        data = data ?: mapOf(),
        datetime = datetimeAction?.toLong()?.let {
            Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.UTC)
        } ?: throw IllegalArgumentException("DatetimeAction is null")
    )
}