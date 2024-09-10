package com.apptank.horus.client.migration.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Represents the data transfer objects (DTOs) used for migration purposes.
 *
 * This sealed class includes different types of DTOs used to handle responses and attributes during migration.
 */
sealed class MigrationDTO {

    //----------------------------------------------------------------------------------------------
    // Response
    //----------------------------------------------------------------------------------------------

    /**
     * Represents the response data for entity schemes during migration.
     *
     * This sealed class contains the DTO for an entity scheme and related attributes.
     */
    sealed class Response {

        /**
         * Represents an entity scheme in the migration response.
         *
         * @param entity The name of the entity.
         * @param type The type of the entity.
         * @param attributes A list of attributes associated with the entity.
         * @param currentVersion The current version of the entity scheme.
         * @constructor Creates a new instance of `EntityScheme` with the specified parameters.
         */
        @Serializable
        data class EntityScheme(
            val entity: String? = null,
            val type: String? = null,
            val attributes: List<AttributeDTO>? = null,
            @SerialName("current_version") val currentVersion: Long? = null
        ) {
            /**
             * Retrieves a list of related entity schemes based on the attributes.
             *
             * This method iterates through the attributes and collects all related entity schemes.
             *
             * @return A list of related `EntityScheme` objects.
             */
            fun getRelated(): List<EntityScheme> {
                val output = mutableListOf<EntityScheme>()

                attributes?.forEach {
                    if (it.related != null) {
                        output.addAll(it.related)
                    }
                }

                return output
            }
        }
    }

    /**
     * Represents an attribute within an entity scheme during migration.
     *
     * @param name The name of the attribute.
     * @param version The version of the attribute.
     * @param type The type of the attribute.
     * @param nullable Indicates if the attribute can be null.
     * @param options Optional values for attributes of type Enum.
     * @param related A list of related entity schemes.
     * @param linkedEntity The name of the linked entity, if applicable (used for relations).
     * @constructor Creates a new instance of `AttributeDTO` with the specified parameters.
     */
    @Serializable
    data class AttributeDTO(
        val name: String? = null,
        val version: Long? = null,
        val type: String? = null,
        val nullable: Boolean? = null,
        val options: List<String>? = null,
        val related: List<Response.EntityScheme>? = null,
        @SerialName("linked_entity") val linkedEntity: String? = null
    )
}
