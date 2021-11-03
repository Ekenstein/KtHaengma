package haengma.core.sgf.models

import haengma.core.utils.*
import kotlinx.serialization.Serializable

@Serializable
data class Point(val x: Int, val y: Int) {
    companion object {
        fun fromTuple(tuple: Pair<Int, Int>) = Point(tuple.x, tuple.y)
    }
}
val Point.asTuple
    get() = x to y

enum class Color {
    Black,
    White
}

sealed class GameResult {
    data class Score(val points: Double) : GameResult()
    object Resignation : GameResult()
    object Time : GameResult()
}

sealed class Move {
    object Pass : Move()
    data class Stone(val point: Point) : Move()
}

fun move(x: Int, y: Int): Move = Move.Stone(Point(x, y))
val pass: Move = Move.Pass

val Move.asPoint
    get() = when (this) {
        Move.Pass -> null
        is Move.Stone -> point
    }

sealed class SgfProperty {
    sealed class Move : SgfProperty() {
        data class Stone(val color: Color, val move: haengma.core.sgf.models.Move) : Move()
    }

    sealed class Setup : SgfProperty() {
        data class Position(val color: Color, val points: Set<Point>) : Setup()
        data class Turn(val color: Color) : Setup()
    }

    sealed class Root : SgfProperty() {
        data class BoardSize(val size: Int) : Root()
        data class Komi(val komi: Double) : Root()
    }

    sealed class NodeAnnotation : SgfProperty() {
        data class Comment(val text: Text) : NodeAnnotation()
    }

    sealed class GameInfo : SgfProperty() {
        data class Result(val color: Color, val result: GameResult) : GameInfo()
        data class Handicap(val numberOfStones: Int) : GameInfo()
        data class BlackPlayerName(val name: SimpleText) : GameInfo()
        data class WhitePlayerName(val name: SimpleText) : GameInfo()
    }
}

data class SgfNode(val properties: Set<SgfProperty>) {
    companion object {
        val empty = SgfNode(emptySet())
    }
}

data class SgfGameTree(val sequence: NonEmptyList<SgfNode>, val trees: List<SgfGameTree>)

fun SgfGameTree.appendNode(node: SgfNode) = copy(
    sequence = sequence + node
)

fun SgfGameTree.prependNode(node: SgfNode) = copy(
    sequence = nelOf(node, sequence)
)

fun SgfGameTree.addProperty(property: SgfProperty) = when (property) {
    is SgfProperty.Move -> addMoveProperty(property)
    is SgfProperty.NodeAnnotation -> addPropertyToLastNode(property)
    is SgfProperty.Root -> addPropertyToRootNode(property)
    is SgfProperty.Setup -> addSetupProperty(property)
    is SgfProperty.GameInfo -> addPropertyToRootNode(property)
}

inline fun <reified T : SgfProperty> SgfNode.addProperty(property: T): SgfNode {
    val existingProperties = properties.filterIsInstance<T>().toSet()
    return copy(
        properties = (properties - existingProperties) + property
    )
}

private fun SgfGameTree.addPropertyToLastNode(property: SgfProperty) = when {
    sequence.isSingle() -> copy(sequence = nelOf(lastNode().addProperty(property)))
    else -> {
        val node = lastNode()
        val sequenceWithoutLastNode = sequence - node
        val newLastNode = node.addProperty(property)
        copy(
            sequence = (sequenceWithoutLastNode + newLastNode).toNelUnsafe()
        )
    }
}

private fun SgfGameTree.addPropertyToRootNode(property: SgfProperty) = when {
    sequence.isSingle() -> copy(sequence = nelOf(rootNode().addProperty(property)))
    !rootNode().hasRootProperties -> prependNode(property.asNode)
    else -> {
        val node = rootNode()
        val sequenceWithoutRootNode = sequence - node
        val newRootNode = node.addProperty(property)
        copy(
            sequence = nelOf(newRootNode, sequenceWithoutRootNode)
        )
    }
}

private fun SgfGameTree.addMoveProperty(property: SgfProperty.Move) = when {
    lastNode().hasSetupProperties -> appendNode(property.asNode)
    property is SgfProperty.Move.Stone && lastNode().hasMove -> appendNode(property.asNode)
    else -> addPropertyToLastNode(property)
}

private fun SgfGameTree.addSetupProperty(property: SgfProperty.Setup) = when {
    lastNode().hasMoveProperties -> appendNode(property.asNode)
    else -> addPropertyToLastNode(property)
}

val SgfProperty.asNode
    get() = SgfNode(setOf(this))

fun SgfGameTree.lastNode() = sequence.last()
fun SgfGameTree.rootNode() = sequence.first()

val SgfNode.hasSetupProperties
    get() = properties.filterIsInstance<SgfProperty.Setup>().any()

val SgfNode.hasMoveProperties
    get() = properties.filterIsInstance<SgfProperty.Move>().any()

val SgfNode.hasRootProperties
    get() = properties.filterIsInstance<SgfProperty.Root>().any()

val SgfNode.hasBlackMove
    get() = properties.filterIsInstance<SgfProperty.Move.Stone>().any { it.color == Color.Black }

val SgfNode.hasWhiteMove
    get() = properties.filterIsInstance<SgfProperty.Move.Stone>().any { it.color == Color.White }

val SgfNode.hasMove
    get() = hasBlackMove || hasWhiteMove

inline fun <reified T : SgfProperty> SgfNode.findProperty() = properties.filterIsInstance<T>().singleOrNull()
