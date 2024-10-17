package org.apptank.horus.client.migration.domain


object AttributeNameMasker {

    /**
     * Masks the attribute name with a prefix based on the attribute type.
     *
     * @param attributeType The type of the attribute.
     * @return The masked attribute name.
     */
    fun maskByType(attributeName: String, attributeType: AttributeType): String {

        val prefix = when (attributeType) {
            AttributeType.PrimaryKeyInteger -> "_pkInt_"
            AttributeType.PrimaryKeyString -> "_pkStr_"
            AttributeType.PrimaryKeyUUID -> "_pkUUID_"
            AttributeType.Integer -> "_int_"
            AttributeType.Float -> "_float_"
            AttributeType.Boolean -> "_bool_"
            AttributeType.String -> "_str_"
            AttributeType.Text -> "_text_"
            AttributeType.RefFile -> "_refFile_"
            AttributeType.Enum -> "_enum_"
            AttributeType.Timestamp -> "_timestamp_"
            AttributeType.UUID -> "_uuid_"
            AttributeType.Json -> "_json_"
            else -> ""
        }

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
        return when {
            columName.contains("_pkInt_") -> AttributeType.PrimaryKeyInteger
            columName.contains("_pkStr_") -> AttributeType.PrimaryKeyString
            columName.contains("_pkUUID_") -> AttributeType.PrimaryKeyUUID
            columName.contains("_int_") -> AttributeType.Integer
            columName.contains("_float_") -> AttributeType.Float
            columName.contains("_bool_") -> AttributeType.Boolean
            columName.contains("_str_") -> AttributeType.String
            columName.contains("_text_") -> AttributeType.Text
            columName.contains("_refFile_") -> AttributeType.RefFile
            columName.contains("_enum_") -> AttributeType.Enum
            columName.contains("_timestamp_") -> AttributeType.Timestamp
            columName.contains("_uuid_") -> AttributeType.UUID
            columName.contains("_json_") -> AttributeType.Json
            else -> throw IllegalArgumentException("Attribute type not found")
        }
    }
}