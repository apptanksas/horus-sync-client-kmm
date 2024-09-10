package com.apptank.horus.client.extensions


/**
 * Applies a given action to each key-value pair in the map.
 *
 * This extension function allows you to iterate over the entries of the map and perform an action on each key-value pair.
 *
 * @param action The function to be applied to each key-value pair in the map. It receives the key and value as parameters.
 */
fun <K, V> Map<K, V>.forEachPair(action: (K, V) -> Unit) {
    for ((k, v) in this) action(k, v)
}
