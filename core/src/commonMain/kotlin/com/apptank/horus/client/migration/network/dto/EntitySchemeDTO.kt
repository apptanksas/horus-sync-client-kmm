package com.apptank.horus.client.migration.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class EntitySchemeDTO(
    val entity: String? = null,
    val type: String? = null,
    val attributes: List<AttributeDTO>? = null,
    val currentVersion: Long? = null
) {
    fun getRelated(): List<EntitySchemeDTO> {
        val output = mutableListOf<EntitySchemeDTO>()

        attributes?.forEach {
            if (it.related != null) {
                output.addAll(it.related)
            }
        }

        return output
    }
}

@Serializable
data class AttributeDTO(
    val name: String? = null,
    val version: Long? = null,
    val type: String? = null,
    val nullable: Boolean? = null,
    val options: List<String>? = null,
    val related: List<EntitySchemeDTO>? = null
)