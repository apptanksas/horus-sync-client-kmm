package org.apptank.horus.client.sync.network.dto

import org.apptank.horus.client.base.DataMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Data Transfer Objects (DTOs) for synchronization-related requests and responses.
 */
sealed class SyncDTO {

    //----------------------------------------------------------------------------------------------
    // Request
    //----------------------------------------------------------------------------------------------

    /**
     * Sealed class representing various types of synchronization requests.
     */
    sealed class Request {

        /**
         * Request to synchronize an action.
         *
         * @param action The type of action (e.g., "INSERT", "UPDATE", "DELETE").
         * @param entity The name of the entity being synchronized.
         * @param data The data to be synchronized.
         * @param actionedAt The timestamp of the action.
         */
        @Serializable
        data class SyncActionRequest(
            private val action: String,
            private val entity: String,
            private val data: DataMap,
            @SerialName("actioned_at") private val actionedAt: Long
        )

        /**
         * Request to validate an entity's hash.
         *
         * @param data The data to be validated.
         * @param hash The expected hash value.
         */
        @Serializable
        data class ValidateHashingRequest(
            val data: DataMap,
            val hash: String
        )

        /**
         * Request to retrieve an entity's hash.
         *
         * @param entity The name of the entity.
         * @param hash The hash value of the entity.
         */
        @Serializable
        data class EntityHash(
            val entity: String,
            val hash: String
        )
    }

    //----------------------------------------------------------------------------------------------
    // Response
    //----------------------------------------------------------------------------------------------

    /**
     * Sealed class representing various types of synchronization responses.
     */
    sealed class Response {

        /**
         * Response containing the hash validation result for an entity.
         *
         * @param entity The name of the entity.
         * @param hashingValidation The result of the hash validation.
         */
        @Serializable
        data class EntityHash(
            var entity: String? = null,
            @SerialName("hash") var hashingValidation: HashingValidation? = null
        )

        /**
         * Response containing an entity's data.
         *
         * @param entity The name of the entity.
         * @param data The data of the entity.
         */
        @Serializable
        data class Entity(
            var entity: String? = null,
            var data: DataMap? = null
        )

        /**
         * Response containing a synchronization action's details.
         *
         * @param action The type of action (e.g., "INSERT", "UPDATE", "DELETE").
         * @param entity The name of the entity.
         * @param data The data associated with the action.
         * @param actionedAt The timestamp when the action was performed.
         * @param syncedAt The timestamp when the action was synchronized.
         */
        @Serializable
        data class SyncAction(
            val action: String? = null,
            val entity: String? = null,
            val data: DataMap? = null,
            @SerialName("actioned_at") val actionedAt: Long? = null,
            @SerialName("synced_at") val syncedAt: Long? = null
        )

        /**
         * Response containing the result of hash validation.
         *
         * @param expected The expected hash value.
         * @param obtained The obtained hash value.
         * @param matched Whether the obtained hash matches the expected hash.
         */
        @Serializable
        data class HashingValidation(
            var expected: String? = null,
            var obtained: String? = null,
            var matched: Boolean? = null
        )

        /**
         * Response containing an entity's ID and hash.
         *
         * @param id The ID of the entity.
         * @param hash The hash value of the entity.
         */
        @Serializable
        data class EntityIdHash(
            val id: String? = null,
            @SerialName("sync_hash") val hash: String? = null
        )
    }
}
