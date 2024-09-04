package com.apptank.horus.client.control

import com.apptank.horus.client.base.DataMap
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

sealed class SyncControl {

    enum class OperationType(val id: Int) {
        HASHING_VALIDATED(1),
        INITIAL(2),
        CHECKPOINT(3),
    }

    enum class Status(val id: Int) {
        COMPLETED(1),
        FAILED(2);

        companion object {
            fun fromId(id: Int): Status =
                entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
        }
    }

    enum class ActionType(val id: Int) {
        INSERT(1),
        UPDATE(2),
        DELETE(3);

        companion object {
            fun fromId(id: Int): ActionType =
                entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
        }
    }

    enum class ActionStatus(val id: Int) {
        PENDING(1),
        COMPLETED(2);

        companion object {
            fun fromId(id: Int): ActionStatus =
                entries.find { it.id == id } ?: throw IllegalArgumentException("Invalid id")
        }
    }

    data class Action(
        val id: Int,
        val action: ActionType,
        val entity: String,
        val status: ActionStatus,
        val data: DataMap,
        val actionedAt: LocalDateTime
    ) {

        fun getActionedAtTimestamp(): Long {
            return actionedAt.toInstant(
                TimeZone.UTC
            ).epochSeconds
        }

        fun getEntityId(): String {
            return data["id"] as String
        }

        fun getEntityAttributes(): DataMap {
            return data["attributes"] as DataMap
        }
    }
}

