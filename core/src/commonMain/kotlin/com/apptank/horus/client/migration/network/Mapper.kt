package com.apptank.horus.client.migration.network

import com.apptank.horus.client.migration.domain.Attribute
import com.apptank.horus.client.migration.domain.AttributeType
import com.apptank.horus.client.migration.domain.EntityScheme
import com.apptank.horus.client.migration.domain.EntityType
import com.apptank.horus.client.migration.exception.InvalidDataSchemeException
import com.apptank.horus.client.migration.network.dto.MigrationDTO

fun MigrationDTO.Response.EntityScheme.toScheme(): EntityScheme {
    return EntityScheme(
        this.entity ?: throw InvalidDataSchemeException(),
        EntityType.valueOf(this.type?.uppercase() ?: EntityType.EDITABLE.name.uppercase()),
        this.attributes?.map { it.toScheme() } ?: listOf(),
        this.currentVersion ?: 1,
        this.getRelated().map { it.toScheme() }
    )
}


fun MigrationDTO.AttributeDTO.toScheme(): Attribute {
    return Attribute(
        this.name ?: throw InvalidDataSchemeException(),
        this.type.toAttributeType(),
        nullable ?: false,
        this.version ?: 1,
        this.options ?: listOf()
    )
}

/**
 * Map attribute type
 */
private fun String?.toAttributeType(): AttributeType {
    return when (this?.lowercase()) {
        "primary_key_integer" -> AttributeType.PrimaryKeyInteger
        "primary_key_string" -> AttributeType.PrimaryKeyString
        "primary_key_uuid" -> AttributeType.PrimaryKeyUUID
        "int" -> AttributeType.Integer
        "float" -> AttributeType.Float
        "boolean" -> AttributeType.Boolean
        "string" -> AttributeType.String
        "text" -> AttributeType.Text
        "json" -> AttributeType.Json
        "enum" -> AttributeType.Enum
        "timestamp" -> AttributeType.Timestamp
        "uuid" -> AttributeType.UUID
        "relation_one_of_many" -> AttributeType.RelationOneOfMany
        "relation_one_of_one" -> AttributeType.RelationOneOfOne
        else -> throw InvalidDataSchemeException("Attribute type $this not defined")
    }
}