package haengma.core.sgf

import haengma.core.sgf.models.*
import haengma.core.utils.Rectangle
import haengma.core.utils.asPoints
import haengma.core.utils.parsers.*

private const val BLACK: Char = 'B'
private const val WHITE: Char = 'W'

private fun <T> propertyValue(valueParser: Parser<T>) = valueParser
    .between(char('['), char(']'))

private val color: Parser<Color> = oneOf(BLACK, WHITE).map {
    when (it) {
        BLACK -> Color.Black
        WHITE -> Color.White
        else -> error("Couldn't recognize the char '$it' as a color.")
    }
}

private val point: Parser<Point> = any
    .token(Char::isLetter)
    .between(skipWhitespaces)
    .assert { it >= 'A' }
    .map {
        when (it) {
            in 'a'..'z' -> it - 'a' + 1
            in 'A'..'Z' -> it - 'A' + 27
            else -> error("The given token '$it' can't be mapped to a point.")
        }
    }
    .repeat(2)
    .map { Point(it[0], it[1]) }

private val compressedPoints: Parser<Set<Point>> = point
    .thenLeft(char(':').between(skipWhitespaces))
    .then(point)
    .map { (upperLeftCorner, lowerRightCorner) ->
        val rectangle = Rectangle(upperLeftCorner.asTuple, lowerRightCorner.asTuple)
        rectangle.asPoints.map(Point::fromTuple).toSet()
    }

private val listOfPoints: Parser<Set<Point>> = compressedPoints.or(point.map { setOf(it) })

private val stone: Parser<Move> = propertyValue(point.map { Move.Stone(it) })
private val pass: Parser<Move> = string("[]").thenRight(returns(Move.Pass))

private val move: Parser<Move> = pass.or(stone)

private fun <A, B> composed(left: Parser<A>, right: Parser<B>): Parser<Pair<A, B>> =
    propertyValue(left.thenLeft(char(':').between(skipWhitespaces)).then(right))

private val lineBreaks = listOf('\n', '\r')

private fun escapedChars(isComposed: Boolean): List<Char> = when (isComposed) {
    true -> escapedChars(false) + listOf(':')
    false -> listOf(']', '\\')
}

private val skipLineBreaks: Parser<Unit> = any.token { it in lineBreaks }.skip()

private fun normalChar(isComposed: Boolean) = any.token { it !in escapedChars(isComposed) }
private fun escapedChar(isComposed: Boolean) = char('\\')
    .between(skipLineBreaks)
    .thenRight(any.token { it in escapedChars(isComposed) }.or(normalChar(isComposed)))

private fun text(isComposed: Boolean): Parser<Text> = any
    .token { it.isWhitespace() && it !in listOf('\n', '\r') }
    .thenReturns(' ')
    .or(normalChar(isComposed))
    .or(escapedChar(isComposed))
    .manyString()
    .map(::textOf)

private fun simpleText(isComposed: Boolean): Parser<SimpleText> = whitespace
    .thenReturns(' ')
    .or(normalChar(isComposed))
    .or(escapedChar(isComposed))
    .manyString()
    .map(::simpleTextOf)

private val propertyIdentifier = any.token(Char::isUpperCase).atLeastOnceString()

private val property: Parser<SgfProperty> = propertyIdentifier
    .flatMap { valueForProperty(it).between(skipWhitespaces) }

private fun valueForProperty(identifier: String): Parser<SgfProperty> = when (identifier) {
    "B" -> move.map { SgfProperty.Move.Stone(Color.Black, it) }
    "W" -> move.map { SgfProperty.Move.Stone(Color.White, it) }
    "C" -> propertyValue(text(false)).map { SgfProperty.NodeAnnotation.Comment(it) }
    "AB" -> propertyValue(listOfPoints)
        .atLeastOnce()
        .map { SgfProperty.Setup.Position(Color.Black, it.flatten().toSet()) }
    "AW" -> propertyValue(listOfPoints)
        .atLeastOnce()
        .map { SgfProperty.Setup.Position(Color.White, it.flatten().toSet()) }
    "SZ" -> propertyValue(int.map { SgfProperty.Root.BoardSize(it) })
    "KM" -> propertyValue(real.map { SgfProperty.Root.Komi(it) })
    "HA" -> propertyValue(int.map { SgfProperty.GameInfo.Handicap(it) })
    "PB" -> propertyValue(simpleText(false).map { SgfProperty.GameInfo.BlackPlayerName(it) })
    "PW" -> propertyValue(simpleText(false).map { SgfProperty.GameInfo.WhitePlayerName(it) })
    "PL" -> propertyValue(color.map { SgfProperty.Setup.Turn(it) })
    else -> error("Couldn't recognize the property identifier '$identifier'.")
}

private val node = char(';')
    .thenRight(property.between(skipWhitespaces).atLeastOnce())
    .map { SgfNode(it.toSet()) }

private val sequence = node.between(skipWhitespaces).atLeastOnce()

private val tree: Parser<SgfGameTree>
    get() = sequence
        .between(skipWhitespaces)
        .then { tree.between(skipWhitespaces).many() }
        .between(char('('), char(')'))
        .map { (sequence, trees) -> SgfGameTree(sequence, trees) }

fun parseSgf(sgf: String): List<SgfGameTree> {
    val (rest, value) = tree
        .between(skipWhitespaces)
        .many()
        .read(sgf)

    check(rest.isEmpty()) {
        "The parser didn't consume the whole string '$rest'."
    }

    return value
}

