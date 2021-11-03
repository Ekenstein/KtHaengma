package utils

import haengma.core.utils.NonEmptyList
import haengma.core.utils.nelOf
import haengma.core.utils.tail
import org.junit.jupiter.api.Assertions.*

fun <T> assertEmpty(iterable: Iterable<T>) = assertIterableEquals(emptyList<T>(), iterable)
fun <T> assertNotEmpty(iterable: Iterable<T>): NonEmptyList<T> = when (iterable.count()) {
    0 -> throw AssertionError("Sequence is empty.")
    else -> nelOf(iterable.first(), iterable.tail())
}

fun <T> assertSingle(iterable: Iterable<T>): T = when (iterable.count()) {
    0 -> throw AssertionError("Sequence contains no elements")
    1 -> iterable.single()
    else -> throw AssertionError("Sequence contains more than one element")
}
