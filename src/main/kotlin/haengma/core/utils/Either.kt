package haengma.core.utils

sealed class Either<out L, out R> {
    data class Left<L>(val value: L) : Either<L, Nothing>()
    data class Right<R>(val value: R) : Either<Nothing, R>()
}

fun <L, R, U> Either<L, R>.mapLeft(block: (L) -> U) = when (this) {
    is Either.Left -> Either.Left(block(value))
    is Either.Right -> this
}

fun <L, R, U> Either<L, R>.mapRight(block: (R) -> U) = when (this) {
    is Either.Left -> this
    is Either.Right -> Either.Right(block(value))
}