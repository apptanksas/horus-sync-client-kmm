package com.apptank.horus.client.extensions

/**
 * Checks if the list does not contain a specific element.
 *
 * @param element The element to check for absence in the list.
 * @return `true` if the element is not in the list; `false` otherwise.
 */
fun <E> List<E>.notContains(element: E): Boolean {
    return !this.contains(element)
}

/**
 * Prepend an element to the list and returns a new list with the element added at the beginning.
 *
 * This function creates a new list by adding the given element at the start of the original list.
 *
 * @param e The element to add at the beginning of the list.
 * @return A new list with the element prepended.
 */
infix fun <T> List<T>.prepend(e: T): List<T> {
    return buildList(this.size + 1) {
        add(e)
        addAll(this@prepend)
    }
}

/**
 * Removes elements from the mutable list that match the given filter condition.
 *
 * This function iterates through the list and removes elements that satisfy the provided condition.
 * Returns `true` if any elements were removed; otherwise, `false`.
 *
 * @param filter The condition to test each element. Elements that match this condition will be removed.
 * @return `true` if at least one element was removed; `false` otherwise.
 */
fun <E> MutableList<E>.removeIf(filter: (E) -> Boolean): Boolean {
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
