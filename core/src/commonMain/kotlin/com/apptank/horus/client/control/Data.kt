package com.apptank.horus.client.control

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant


enum class SyncOperationType(val id: Int) {
    HASHING_VALIDATED(1),
    INITIAL(2),
    CHECKPOINT(3),
}

enum class ControlStatus(val id: Int) {
    COMPLETED(1),
    FAILED(2);

    companion object {
        fun fromId(id: Int): ControlStatus =
            entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
    }
}

enum class SyncActionType(val id: Int) {
    INSERT(1),
    UPDATE(2),
    DELETE(3);

    companion object {
        fun fromId(id: Int): SyncActionType =
            entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
    }
}

enum class SyncActionStatus(val id: Int) {
    PENDING(1),
    COMPLETED(2);

    companion object {
        fun fromId(id: Int): SyncActionStatus =
            entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
    }
}

data class SyncAction(
    val id: Int,
    val action: SyncActionType,
    val entity: String,
    val status: SyncActionStatus,
    val data: Map<String, Any>,
    val datetime: LocalDateTime
) {

    fun getDatetimeAsTimestamp(): Long {
        return datetime.toInstant(
            TimeZone.UTC
        ).epochSeconds
    }
}