package haengma.core.utils

fun Iterable<*>.isSingle() = count() == 1

/**
 * Converts a nullable value to a singleton or an empty list.
 */
fun <T> T?.asList() = when (this) {
    null -> emptyList()
    else -> listOf(this)
}

fun <T> Iterable<T>.tail() = drop(1)