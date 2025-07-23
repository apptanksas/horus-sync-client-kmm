package org.apptank.horus.client.data

/**
 * A sealed class representing internal models related to entity hashing and validation.
 */
internal sealed class InternalModel {

    /**
     * Represents an entity identified by an ID and associated with a hash.
     *
     * @property id The unique identifier of the entity.
     * @property hash The hash value associated with the entity.
     */
    data class EntityIdHash(
        val id: String,
        val hash: String
    )

    /**
     * Represents the result of a hash validation operation for an entity.
     *
     * @property entity The name of the entity being validated.
     * @property hashExpected The expected hash value for the entity.
     * @property hashObtained The hash value obtained from the entity.
     * @property isHashMatched A boolean indicating whether the obtained hash matches the expected hash.
     */
    data class EntityHashValidation(
        val entity: String,
        val hashExpected: String,
        val hashObtained: String,
        val isHashMatched: Boolean
    )

    data class TableEntity(
        val name: String,
        val isWritable: Boolean,
        val level: Int
    )
}
