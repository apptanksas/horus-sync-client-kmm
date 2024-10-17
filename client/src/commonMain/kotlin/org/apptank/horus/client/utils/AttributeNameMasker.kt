package org.apptank.horus.client.utils

import org.apptank.horus.client.migration.domain.AttributeType


object AttributeNameMasker {

    private val prefixMap = mapOf<AttributeType, String>(
        AttributeType.PrimaryKeyInteger to "_pkInt_",
        AttributeType.PrimaryKeyString to "_pkStr_",
        AttributeType.PrimaryKeyUUID to "_pkUUID_",
        AttributeType.Integer to "_int_",
        AttributeType.Float to "_float_",
        AttributeType.Boolean to "_bool_",
        AttributeType.String to "_str_",
        AttributeType.Text to "_text_",
        AttributeType.RefFile to "_refFile_",
        AttributeType.Enum to "_enum_",
        AttributeType.Timestamp to "_timestamp_",
        AttributeType.UUID to "_uuid_",
        AttributeType.Json to "_json_"
    )

    /**
     * Masks the attribute name with a prefix based on the attribute type.
     *
     * @param attributeType The type of the attribute.
     * @return The masked attribute name.
     */
    fun maskByType(attributeName: String, attributeType: AttributeType): String {
        val prefix = prefixMap[attributeType] ?: ""
        return "$prefix$attributeName"
    }

    /**
     * Unmasks the attribute name by removing the prefix.
     *
     * @param attributeName The masked attribute name.
     * @return The original attribute name.
     */
    fun unmask(attributeName: String): String {
        return attributeName.replaceFirst("_.*_".toRegex(), "")
    }


    /**
     * Gets the attribute type based on the attribute name.
     *
     * @param columName The name of the attribute.
     * @return The type of the attribute.
     */
    fun getAttributeType(columName: String): AttributeType {
        val prefix = columName.replaceFirst("_.+_".toRegex(), "")
        return prefixMap.entries.first { it.value == prefix }.key
    }
}