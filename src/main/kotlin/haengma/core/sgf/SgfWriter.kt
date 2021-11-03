package haengma.core.sgf

import haengma.core.sgf.models.*

fun List<SgfGameTree>.toSgf() = StringBuilder().also { b ->
    forEach { it.toSgf(b) }
}.toString()

private fun SgfGameTree.toSgf(builder: StringBuilder): String {
    builder.append("(")
    sequence.forEach { it.toSgf(builder) }
    trees.forEach { it.toSgf(builder) }
    builder.append(")")

    return builder.toString()
}

private fun SgfNode.toSgf(builder: StringBuilder) = builder.append(";").also { b ->
    properties.forEach { it.toSgf(b) }
}

private fun SgfProperty.toSgf(builder: StringBuilder) = when (this) {
    is SgfProperty.Root.BoardSize -> builder.appendProperty("SZ", size.toString())
    is SgfProperty.NodeAnnotation.Comment -> builder.appendProperty("C", text.toSgf(false))
    is SgfProperty.Setup.Position -> when (color) {
        Color.Black -> builder.appendProperty("AB", points.map { it.toSgf() })
        Color.White -> builder.appendProperty("AW", points.map { it.toSgf() })
    }
    is SgfProperty.Move.Stone -> when (color) {
        Color.Black -> builder.appendProperty("B", move.toSgf())
        Color.White -> builder.appendProperty("W", move.toSgf())
    }
    is SgfProperty.Root.Komi -> builder.appendProperty("KM", komi.toString())
    is SgfProperty.GameInfo.Result -> builder.appendProperty("RE", result.toSgf(color))
    is SgfProperty.GameInfo.Handicap -> builder.appendProperty("HA", numberOfStones.toString())
    is SgfProperty.GameInfo.BlackPlayerName -> builder.appendProperty("PB", name.toSgf(false))
    is SgfProperty.GameInfo.WhitePlayerName -> builder.appendProperty("PW", name.toSgf(false))
    is SgfProperty.Setup.Turn -> builder.appendProperty("PL", color.toSgf())
}

private fun GameResult.toSgf(color: Color): String {
    val builder = StringBuilder()
    when (color) {
        Color.Black -> builder.append("B")
        Color.White -> builder.append("W")
    }

    when (this) {
        GameResult.Resignation -> builder.append("+R")
        is GameResult.Score -> builder.append("+").append(points)
        GameResult.Time -> builder.append("+T")
    }

    return builder.toString()
}

private fun escapeText(s: String, isComposed: Boolean): String {
    val escapedChars = listOf('\\', ']') + listOf(':').filter { isComposed }
    return s.flatMap {
        when (it) {
            in escapedChars -> listOf('\\', it)
            else -> listOf(it)
        }
    }.joinToString("")
}

private fun SimpleText.toSgf(isComposed: Boolean) = escapeText(text, isComposed)
private fun Text.toSgf(isComposed: Boolean) = escapeText(text, isComposed)
private fun Point.toSgf(): String {
    val builder = StringBuilder()
    fun intToChar(n: Int) = when {
        n > 26 -> ((n % 27) + 'A'.code).toChar()
        else -> ((n - 1) + 'a'.code).toChar()
    }

    builder.append(intToChar(x)).append(intToChar(y))
    return builder.toString()
}

private fun Color.toSgf() = when (this) {
    Color.Black -> "B"
    Color.White -> "W"
}

private fun Move.toSgf() = when (this) {
    Move.Pass -> ""
    is Move.Stone -> point.toSgf()
}

private fun StringBuilder.appendProperty(identifier: String, value: String) = appendProperty(identifier, listOf(value))

private fun StringBuilder.appendProperty(identifier: String, value: List<String>) = append(identifier)
    .apply {
        if (value.isEmpty()) {
            append("[]")
        } else {
            value.forEach {
                append("[").append(it).append("]")
            }
        }
    }