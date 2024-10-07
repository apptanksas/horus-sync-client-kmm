package org.apptank.horus.client.base

import org.apptank.horus.client.serialization.AnySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apptank.horus.client.eventbus.Event

/**
 * Typealias for a map that holds data where the values are of any type, serializable using `AnySerializer`.
 * This is commonly used to handle dynamic or flexible data structures in JSON format.
 */
typealias DataMap = Map<String, @Serializable(with = AnySerializer::class) Any?>

/**
 * Typealias for a simple callback function that takes no arguments and returns no value.
 */
typealias Callback = () -> Unit

/**
 * Typealias for a nullable callback function that takes no arguments and returns no value.
 */
typealias CallbackNullable = Callback?

/**
 * Typealias for a callback function that takes an [Event] as an argument and returns no value.
 */
internal typealias CallbackEvent = (Event) -> Unit

/**
 * Extension function for DataMap that serializes the map into a JSON string.
 *
 * @return The JSON representation of the DataMap.
 */
internal fun DataMap.encodeToJSON(): String {
    return Json.encodeToString(this)
}

/**
 * Extension function for String that deserializes a JSON string into a DataMap.
 *
 * @return The deserialized DataMap from the JSON string.
 */
internal fun String.decodeToMapAttributes(): DataMap {
    return Json.decodeFromString(this)
}
