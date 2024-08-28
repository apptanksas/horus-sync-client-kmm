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

fun<E> MutableList<E>.removeIf(filter: (E) -> Boolean): Boolean {
    val it = iterator()
    var removed = false
    while (it.hasNext()) {
        if (filter(it.next())) {
            it.remove()
            removed = true
        }
    }
    return removed
}