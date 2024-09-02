package com.apptank.horus.client.database

import com.apptank.horus.client.base.DataMap
import com.apptank.horus.client.serialization.AnySerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf

data class Column(
    val position: Int,
    val name: String,
    val type: String,
    val nullable: Boolean,
)


data class Cursor(
    val index: Int,
    val table: String,
    val values: List<CursorValue<*>>
) {
    fun <T> getValue(attribute: String): T {
        return values.first { it.column.name == attribute }.value as T
    }

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

data class CursorValue<T>(
    val value: T,
    val column: Column
)