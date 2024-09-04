package com.apptank.horus.client.migration.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


sealed class MigrationDTO {

    //----------------------------------------------------------------------------------------------
    // Response
    //----------------------------------------------------------------------------------------------

    sealed class Response {

        @Serializable
        data class EntityScheme(
            val entity: String? = null,
            val type: String? = null,
            val attributes: List<AttributeDTO>? = null,
            @SerialName("current_version") val currentVersion: Long? = null
        ) {
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


