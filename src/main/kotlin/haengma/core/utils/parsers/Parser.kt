package haengma.core.utils.parsers

import haengma.core.utils.NonEmptyList
import haengma.core.utils.toNelUnsafe

fun interface Parser<T> {
    fun read(input: String): Pair<String, T>
}

open class ParseException(override val message: String) : Exception(message)

val any: Parser<Char> = Parser { input ->
    when (input) {
        "" -> throw ParseException("EOF")
        else -> input.drop(1) to input.first()
    }
}

fun <T> returns(value: T): Parser<T> = Parser { input -> input to value }

fun <T> fail(message: String): Parser<T> = Parser {
    throw ParseException(message)
}

fun <T, U> Parser<T>.flatMap(block: (T) -> Parser<U>): Parser<U> = Parser { input ->
    val (rest, value) = read(input)
    block(value).read(rest)
}

fun <T, U> Parser<T>.map(block: (T) -> U): Parser<U> = flatMap { returns(block(it)) }

fun oneOf(vararg chars: Char): Parser<Char> = any.token { it in chars }
fun char(char: Char) = oneOf(char)

fun string(s: String) = s.fold(returns(Unit)) { parser, c ->
    parser.thenLeft(char(c))
}.map { s }

fun <T> Parser<T>.repeat(times: Int): Parser<List<T>> = Parser { input ->
    fun parse(items: List<T>, input: String, times: Int): Pair<String, List<T>> {
        if (times <= 0) {
            return input to items
        }

        val (rest, value) = read(input)
        return parse(items + value, rest, times - 1)
    }

    parse(emptyList(), input, times)
}

fun <T> Parser<T>.token(predicate: (T) -> Boolean): Parser<T> = flatMap {
    if (predicate(it)) {
        returns(it)
    } else {
        fail("The predicate didn't hold for '$it'")
    }
}

fun <T> Parser<T>.or(parser: Parser<T>): Parser<T> = Parser { input ->
    try {
        read(input)
    } catch (ex: ParseException) {
        parser.read(input)
    }
}

fun <T> Parser<T>.many(): Parser<List<T>> = Parser { input ->
    fun parse(items: List<T>, rest: String): Pair<String, List<T>> = try {
        val result = read(rest)
        parse(items + result.second, result.first)
    } catch (ex: ParseException) {
        rest to items
    }

    parse(emptyList(), input)
}

fun <T> Parser<T>.atLeastOnce(): Parser<NonEmptyList<T>> = many()
    .assert { it.isNotEmpty() }
    .map { it.toNelUnsafe() }

fun <T> Parser<T>.assert(predicate: (T) -> Boolean): Parser<T> = flatMap {
    if (!predicate(it)) {
        fail("The value didn't satisfy the predicate")
    } else {
        returns(it)
    }
}

fun Parser<Char>.manyString(): Parser<String> = many().map { it.joinToString("") }
fun Parser<Char>.atLeastOnceString(): Parser<String> = atLeastOnce().map { it.joinToString("") }

fun <T, U> Parser<T>.then(parser: Parser<U>): Parser<Pair<T, U>> = flatMap { left ->
    parser.map { right -> left to right }
}

fun <T, U> Parser<T>.then(parser: (T) -> Parser<U>) = flatMap { left ->
    parser(left).map { right -> left to right }
}

fun <T, U> Parser<T>.thenLeft(parser: Parser<U>): Parser<T> = then(parser).map { (left, _) -> left }
fun <T, U> Parser<T>.thenRight(parser: Parser<U>): Parser<U> = then(parser).map { (_, right) -> right }

fun <T> Parser<T>.between(left: Parser<*>, right: Parser<*>): Parser<T> = left
    .thenRight(this)
    .thenLeft(right)

fun <T> Parser<T>.between(parser: Parser<*>): Parser<T> = between(parser, parser)

fun Parser<*>.skip(): Parser<Unit> = many().map { }

val whitespace: Parser<Char> = any.token { it.isWhitespace() }

val skipWhitespaces: Parser<Unit> = whitespace.skip()

val int: Parser<Int> = any.token(Char::isDigit).atLeastOnceString().map { it.toInt() }
val real: Parser<Double> = int.thenLeft(char('.')).then(int).map { (n1, n2) -> ("$n1.$n2").toDouble() }

fun <T> Parser<*>.thenReturns(value: T): Parser<T> = thenRight(returns(value))