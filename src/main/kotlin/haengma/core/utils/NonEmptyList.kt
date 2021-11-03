package haengma.core.utils

interface NonEmptyList<out T> : List<T> {
    val head: T
    val tail: List<T>

    override fun isEmpty(): Boolean = false
}

fun <T> nelOf(head: T, vararg tail: T): NonEmptyList<T> = nelOf(head, tail.toList())

fun <T> nelOf(head: T, tail: List<T>): NonEmptyList<T> = when {
    tail.isEmpty() -> Singleton(head)
    else -> NonEmptyListImpl(head, tail)
}

fun <T> nelOf(head: T): NonEmptyList<T> = Singleton(head)

fun <T> List<T>.toNel(): NonEmptyList<T>? = when {
    isEmpty() -> null
    size == 1 -> Singleton(get(0))
    else -> NonEmptyListImpl(get(0), drop(1))
}

fun <T> List<T>.toNelUnsafe() = toNel()
    ?: error("The list is empty.")

operator fun <T> NonEmptyList<T>.plus(item: T) = plus(nelOf(item))
operator fun <T> NonEmptyList<T>.plus(items: NonEmptyList<T>) = (toList() + items).toNelUnsafe()

private class NonEmptyListImpl<out T>(
    override val head: T,
    override val tail: List<T>
) : NonEmptyList<T> {
    private val all by lazy {
        listOf(head) + tail
    }

    override val size: Int = all.size

    override fun contains(element: @UnsafeVariance T): Boolean = when (element) {
        head -> true
        else -> tail.contains(element)
    }

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean =
        all.containsAll(elements)

    override fun get(index: Int): T = when (index) {
        0 -> head
        else -> all[index]
    }

    override fun indexOf(element: @UnsafeVariance T): Int = when (element) {
        head -> 0
        else -> all.indexOf(element)
    }

    override fun iterator(): Iterator<T> = all.iterator()

    override fun lastIndexOf(element: @UnsafeVariance T): Int = all.lastIndexOf(element)

    override fun listIterator(): ListIterator<T> = all.listIterator()

    override fun listIterator(index: Int): ListIterator<T> = all.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<T> = all.subList(fromIndex, toIndex)

    override fun toString(): String = all.toString()

    override fun equals(other: Any?): Boolean = all == other
}

private class Singleton<out T>(override val head: T) : NonEmptyList<T> {
    override val size: Int = 1

    override fun contains(element: @UnsafeVariance T): Boolean = element == head

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean =
        elements.size == 1 && elements.contains(head)

    override fun get(index: Int): T = when (index) {
        0 -> head
        else -> throw IndexOutOfBoundsException(index)
    }

    override fun indexOf(element: @UnsafeVariance T): Int = when (element) {
        head -> 0
        else -> -1
    }

    override fun iterator(): Iterator<T> = SingletonIterator()

    override fun lastIndexOf(element: @UnsafeVariance T): Int = when (element) {
        head -> 0
        else -> -1
    }

    override fun listIterator(): ListIterator<T> = SingletonIterator()

    override fun listIterator(index: Int): ListIterator<T> = when {
        index < 0 || index > size -> throw IndexOutOfBoundsException(index)
        else -> SingletonIterator()
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> = when {
        fromIndex > 0 || toIndex > 0 -> throw IndexOutOfBoundsException()
        else -> this
    }

    override val tail: List<T> = emptyList()

    private inner class SingletonIterator : ListIterator<T> {
        private var hasNext = true

        override fun hasNext(): Boolean = hasNext

        override fun hasPrevious(): Boolean = false

        override fun next(): T = when (hasNext) {
            true -> {
                hasNext = false
                head
            }
            false -> throw NoSuchElementException()
        }

        override fun nextIndex(): Int = 0

        override fun previous(): T = throw NoSuchElementException()

        override fun previousIndex(): Int = -1
    }

    override fun toString(): String = listOf(head).toString()

    override fun equals(other: Any?): Boolean = listOf(head) == other
}