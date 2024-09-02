package com.apptank.horus.client.base

import com.apptank.horus.client.serialization.AnySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal typealias DataMap = Map<String, @Serializable(with = AnySerializer::class) Any?>

fun DataMap.encodeToJSON(): String {
    return Json.encodeToString(this)
}

fun String.decodeToMapAttributes(): DataMap {
    return Json.decodeFromString(this)
}