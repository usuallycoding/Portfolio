package com.usuallycoding.portfolio.admin.presentation

/** Returns a copy with the element at [index] moved by [delta] places, or the same list if that is impossible. */
fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices || delta == 0) return this
    val copy = toMutableList()
    val item = copy.removeAt(index)
    copy.add(target, item)
    return copy
}
