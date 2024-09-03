package com.apptank.horus.client.sync.network.dto

import com.apptank.horus.client.base.DataMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


sealed class SyncDTO {

    //----------------------------------------------------------------------------------------------
    // Request
    //----------------------------------------------------------------------------------------------

    sealed class Request {

        @Serializable
        data class SyncActionRequest(
            private val action: String,
            private val entity: String,
            private val data: DataMap,
            private val datetime: Long
        )


        @Serializable
        data class ValidateHashingRequest(
            val data: DataMap,
            val hash: String
        )
    }

    //----------------------------------------------------------------------------------------------
    // Response
    //----------------------------------------------------------------------------------------------
    sealed class Response {

        @Serializable
        data class EntityHash(
            var entity: String? = null,
            var hash: HashingValidation? = null
        )

        @Serializable
        data class Entity(
            var entity: String? = null,
            var data: DataMap? = null
        )


        @Serializable
        data class SyncAction(
            val action: String? = null,
            val entity: String? = null,
            val data: DataMap? = null,
            @SerialName("actioned_at") val actionedAt: Long? = null,
            @SerialName("synced_at") val syncedAt: Long? = null
        )

        @Serializable
        data class HashingValidation(
            var expected: String? = null,
            var obtained: String? = null,
            var matched: Boolean? = null
        )

        @Serializable
        data class EntityIdHash(
            val id: String? = null,
            @SerialName("sync_hash") val hash: String? = null
        )

    }

    @Serializable
    @Deprecated("Create another model")
    data class EntityHash(
        val entity: String,
        val hash: String
    )

}

