package haengma.core.sgf.models

sealed interface SimpleText {
    val text: String
}

@JvmInline
private value class SimpleTextImpl(override val text: String) : SimpleText

fun simpleTextOf(s: String): SimpleText {
    fun replaceWhitespaceWithSpace(c: Char): Char = when {
        c.isWhitespace() -> ' '
        else -> c
    }

    val simpleText = s.map(::replaceWhitespaceWithSpace).joinToString("")

    return SimpleTextImpl(simpleText)
}

operator fun SimpleText.plus(other: SimpleText): SimpleText = SimpleTextImpl(text + other.text)