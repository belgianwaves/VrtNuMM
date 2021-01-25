package com.bw.vrtnumm.shared.utils

public fun <T> List<T>.last(): T {
    if (isEmpty())
        throw NoSuchElementException("List is empty.")
    return this[this.size - 1]
}