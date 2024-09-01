package com.apptank.horus.client.extensions


fun <K, V> Map<K, V>.forEachPair(action: (K, V) -> Unit) {
    for ((k, v) in this) action(k, v)
}

