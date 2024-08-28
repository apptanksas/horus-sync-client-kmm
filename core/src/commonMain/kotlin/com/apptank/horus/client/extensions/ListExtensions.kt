package com.apptank.horus.client.extensions

fun <E> List<E>.notContains(element: E): Boolean {
    return !this.contains(element)
}

infix fun <T> List<T>.prepend(e: T): List<T> {
    return buildList(this.size + 1) {
        add(e)
        addAll(this@prepend)
    }
}