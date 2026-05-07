package org.apptank.horus.client.database.struct

import org.apptank.horus.client.base.DataMap
import org.apptank.horus.client.serialization.AnySerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import org.apptank.horus.client.migration.domain.AttributeType

/**
 * Represents a column in a database table.
 *
 * @property position The position of the column in the table.
 * @property name The name of the column.
 * @property type The data type of the column.
 * @property nullable Indicates whether the column can have null values.
 * @property format The format of the column value.
 */
internal data class Column(
    val position: Int,
    val name: String,
    val type: String,
    val nullable: Boolean,
    val format: AttributeType? = null
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
     * Retrieves the value of a specified column, coercing numeric types as needed.
     *
     * On Kotlin/Native (iOS), SQLite INTEGER columns are always stored as Long. Without
     * type coercion, requesting the value as Int causes a ClassCastException on iOS.
     * Using a reified type parameter allows detecting the expected type at runtime and
     * converting accordingly.
     *
     * @param attribute The name of the column to retrieve the value from.
     * @return The value of the specified column, coerced to T if needed.
     */
    inline fun <reified T> getValue(attribute: String): T {
        val raw = values.first { it.column.name == attribute }.value
        return coerceValue<T>(raw)
    }

    /**
     * Retrieves the value of a specified column or null if the column does not exist,
     * coercing numeric types as needed.
     *
     * @param attribute The name of the column to retrieve the value from.
     * @return The value of the specified column or null if the column does not exist.
     */
    inline fun <reified T> getValueOrNull(attribute: String): T? {
        val raw = values.firstOrNull { it.column.name == attribute }?.value ?: return null
        return coerceValue<T>(raw)
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

    /**
     * Coerces [raw] to type [T], handling numeric type mismatches that occur on
     * iOS/Kotlin Native where SQLite INTEGER columns are always stored as [Long].
     *
     * Supported conversions:
     * - [Long]   → [Int], [Short], [Byte], [Float], [Double]
     * - [Int]    → [Long]
     * - [Double] → [Float]
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> coerceValue(raw: Any?): T {

        if (raw == null) return unsafeCast(null)

        return when (T::class) {
            Int::class -> when (raw) {
                is Long -> raw.toInt(); is Double -> raw.toInt(); else -> raw
            }

            Long::class -> when (raw) {
                is Int -> raw.toLong(); is Double -> raw.toLong(); else -> raw
            }

            Short::class -> when (raw) {
                is Long -> raw.toShort(); is Int -> raw.toShort(); else -> raw
            }

            Byte::class -> when (raw) {
                is Long -> raw.toByte(); is Int -> raw.toByte(); else -> raw
            }

            Float::class -> when (raw) {
                is Double -> raw.toFloat(); is Long -> raw.toFloat(); is Int -> raw.toFloat(); else -> raw
            }

            Double::class -> when (raw) {
                is Long -> raw.toDouble(); is Int -> raw.toDouble(); is Float -> raw.toDouble(); else -> raw
            }

            else -> raw
        } as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> unsafeCast(value: Any?): T = value as T

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
    val value: T?,
    val column: Column
)
