package org.apptank.horus.client.migration.network

import org.apptank.horus.client.migration.domain.Attribute
import org.apptank.horus.client.migration.domain.AttributeType
import org.apptank.horus.client.migration.domain.EntityScheme
import org.apptank.horus.client.migration.domain.EntityType
import org.apptank.horus.client.migration.exception.InvalidDataSchemeException
import org.apptank.horus.client.migration.network.dto.MigrationDTO

/**
 * Converts a `MigrationDTO.Response.EntityScheme` to an `EntityScheme`.
 *
 * This function maps the properties of `MigrationDTO.Response.EntityScheme` to an `EntityScheme` object,
 * converting attributes and related entities to their corresponding types. It throws an `InvalidDataSchemeException`
 * if essential properties are missing.
 *
 * @return An `EntityScheme` object representing the converted data.
 * @throws InvalidDataSchemeException If the entity name is null.
 */
internal fun MigrationDTO.Response.EntityScheme.toScheme(): EntityScheme {

    val type = EntityType.valueOf(this.type?.uppercase() ?: EntityType.WRITABLE.name)

    return EntityScheme(
        this.entity ?: throw InvalidDataSchemeException(),
        type,
        this.attributes?.map { it.toScheme() } ?: listOf(),
        this.currentVersion ?: 1,
        this.getRelated().map { it.toScheme() }
    )
}

/**
 * Converts a `MigrationDTO.AttributeDTO` to an `Attribute`.
 *
 * This function maps the properties of `MigrationDTO.AttributeDTO` to an `Attribute` object, converting the attribute type
 * and handling optional properties. It throws an `InvalidDataSchemeException` if the attribute name is null.
 *
 * @return An `Attribute` object representing the converted data.
 * @throws InvalidDataSchemeException If the attribute name is null.
 */
internal fun MigrationDTO.AttributeDTO.toScheme(): Attribute {
    return Attribute(
        this.name ?: throw InvalidDataSchemeException(),
        this.type.toAttributeType(),
        nullable ?: false,
        this.version ?: 1,
        this.options ?: listOf(),
        this.linkedEntity,
        this.deleteOnCascade ?: true,
        this.regex
    )
}

/**
 * Maps a string representation of an attribute type to the corresponding `AttributeType`.
 *
 * This function converts a string value (case-insensitive) to an `AttributeType` enum. It throws an `InvalidDataSchemeException`
 * if the attribute type is not defined.
 *
 * @return The corresponding `AttributeType` enum value.
 * @throws InvalidDataSchemeException If the attribute type string is not recognized.
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
        "custom" -> AttributeType.Custom
        "timestamp" -> AttributeType.Timestamp
        "uuid" -> AttributeType.UUID
        "ref_file" -> AttributeType.RefFile
        "relation_one_of_many" -> AttributeType.RelationOneOfMany
        "relation_one_of_one" -> AttributeType.RelationOneOfOne
        else -> throw InvalidDataSchemeException("Attribute type $this not defined")
    }
}
