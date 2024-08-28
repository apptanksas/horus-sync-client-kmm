package com.apptank.horus.client.extensions

import app.cash.sqldelight.db.SqlCursor
import kotlinx.serialization.json.Json

private val decoder = Json { ignoreUnknownKeys = true }

fun SqlCursor.jsonToMap(attributeName: String): Map<String, Any> {
    return decoder.decodeFromString<Map<String, Any>>(this.getValue(attributeName))
}