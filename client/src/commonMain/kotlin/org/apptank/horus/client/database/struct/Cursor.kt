package org.apptank.horus.client.database.struct

import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.serialization.AnySerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf

/**
 * Represents a column in a database table.
 *
 * @property position The position of the column in the table.
 * @property name The name of the column.
 * @property type The data type of the column.
 * @property nullable Indicates whether the column can have null values.
 */
internal data class Column(
    val position: Int,
    val name: String,
    val type: String,
    val nullable: Boolean
)

/**
 * Represents a cursor that points to a row in a database table.
 *
 * @property index The index of the cursor in the result set.
 * @property table The name of the table that the cursor is pointing to.
 * @property values A list of [CursorValue] objects representing the column values of the current row.
 *
 * @param T The type of value expected for the specified attribute.
 */
internal data class Cursor(
    val index: Int,
    val table: String,
    val values: List<CursorValue<*>>
) {
    /**
     * Retrieves the value of a specified column.
     *
     * @param attribute The name of the column to retrieve the value from.
     * @return The value of the specified column.
     */
    fun <T> getValue(attribute: String): T {
        return values.first { it.column.name == attribute }.value as T
    }

    /**
     * Retrieves the value of a specified column and converts it to a [DataMap] using JSON decoding.
     *
     * @param attributeName The name of the column to retrieve the value from.
     * @return The converted [DataMap] object.
     */
    fun getStringAndConvertToMap(attributeName: String): DataMap {
        val value = getValue<String>(attributeName)
        return decoder.decodeFromString<DataMap>(value)
    }

    private companion object {
        private val decoder = Json {
            ignoreUnknownKeys = true
            serializersModule = serializersModuleOf(Any::class, AnySerializer)
        }
    }
}

/**
 * Represents a value of a column in a row of a database table.
 *
 * @property value The value of the column.
 * @property column The [Column] object representing the column metadata.
 *
 * @param T The type of the column value.
 */
internal data class CursorValue<T>(
    val value: T,
    val column: Column
)
