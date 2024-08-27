package com.apptank.horus.client.migration.service.dto

import com.apptank.horus.client.migration.models.EntityType
import kotlinx.serialization.Serializable

@Serializable
data class EntitySchemeDTO(
    val entity: String? = null,
    val type: String? = null,
    val attributes: List<AttributeDTO>? = null,
    val currentVersion: Int? = null
) {
    // TODO("Get Related") fun getRelated(): List<EntitySchemeDTO> = attributes?.map { it.related.map { it.getRelated() } }

    fun getType(): EntityType = EntityType.valueOf(type ?: EntityType.EDITABLE.name)
}

@Serializable
data class AttributeDTO(
    val name: String? = null,
    val version: Int? = null,
    val type: String? = null,
    val nullable: Boolean? = null,
    val related: List<EntitySchemeDTO>? = null
)