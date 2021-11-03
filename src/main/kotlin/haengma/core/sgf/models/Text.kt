package haengma.core.sgf.models

sealed interface Text {
    val text: String
}

private data class TextImpl(override val text: String) : Text

fun textOf(s: String): Text {
    val legalWhitespaces = listOf('\n', '\r', ' ')
    fun replaceIllegalWhitespacesWithSpace(c: Char): Char = when {
        c.isWhitespace() && c !in legalWhitespaces -> ' '
        else -> c
    }

    val text = s.map(::replaceIllegalWhitespacesWithSpace).joinToString("")
    return TextImpl(text)
}

fun Text.appendLine(text: Text) = textOf(
    "${this.text}\n${text.text}"
)