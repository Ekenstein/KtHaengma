package haengma.core.game

import haengma.core.sgf.models.*
import haengma.core.utils.asList
import haengma.core.utils.isSingle
import haengma.core.utils.nelOf
import haengma.core.utils.toNelUnsafe
import kotlin.math.ceil

val SgfGameTree.stones
    get() = sequence.flatMap { it.properties }.flatMap(SgfProperty::getMoves)

private fun SgfProperty.getMoves() = when (this) {
    is SgfProperty.Move.Stone -> move.asPoint.asList().map { Stone(color, it) }
    is SgfProperty.Setup.Position -> points.map { Stone(color, it) }
    is SgfProperty.Root.BoardSize,
    is SgfProperty.Root.Komi,
    is SgfProperty.GameInfo.Result,
    is SgfProperty.GameInfo.Handicap,
    is SgfProperty.GameInfo.WhitePlayerName,
    is SgfProperty.GameInfo.BlackPlayerName,
    is SgfProperty.Setup.Turn,
    is SgfProperty.NodeAnnotation.Comment -> emptyList()
}

data class Stone(val color: Color, val point: Point)

data class Board(
    val stones: List<Stone>,
    val size: Int,
    val blackCaptures: Int,
    val whiteCaptures: Int
) {
    companion object {
        fun empty(size: Int) = Board(
            stones = emptyList(),
            size = size,
            blackCaptures = 0,
            whiteCaptures = 0
        )
    }
}

fun Board.print(): String {
    val builder = StringBuilder()
    for (y in 1..size) {
        for (x in 1..size) {
            val point = Point(x, y)
            when (stones.filter { it.point == point }.map { it.color }.singleOrNull()) {
                Color.Black -> builder.append('#')
                Color.White -> builder.append('O')
                null -> builder.append('.')
            }
        }
        builder.appendLine()
    }

    return builder.toString()
}

fun Board.isOccupied(point: Point) = stones.any { it.point == point }

private fun Stone.adjacentPoints(size: Int) = listOf(
    Point(point.x, point.y - 1),
    Point(point.x, point.y + 1),
    Point(point.x + 1, point.y),
    Point(point.x - 1, point.y)
).filter { (x, y) -> x in 1..size && y in 1..size }

fun SgfGameTree.addMove(color: Color, move: Move, size: Int): SgfGameTree = when (move) {
    Move.Pass -> addProperty(SgfProperty.Move.Stone(color, move))
    is Move.Stone -> placeStone(color, move.point, size)
}

fun SgfGameTree.resign(color: Color): SgfGameTree = addProperty(
    SgfProperty.GameInfo.Result(
        color.inverse, GameResult.Resignation
    )
)

fun SgfGameTree.comment(comment: Text): SgfGameTree {
    val existingComment = lastNode().findProperty<SgfProperty.NodeAnnotation.Comment>()
    val newComment = existingComment?.text?.appendLine(comment) ?: comment

    return addProperty(
        SgfProperty.NodeAnnotation.Comment(newComment)
    )
}

private fun SgfGameTree.placeStone(color: Color, point: Point, size: Int): SgfGameTree {
    val stone = Stone(color, point)
    checkIllegalMove(!isBoardRepeating(stone, size)) {
        "The board is repeating."
    }

    val board = toBoard(size)

    checkIllegalMove(!board.isOccupied(point)) {
        "The point (${point.x};${point.y}) is occupied."
    }

    val updatedBoard = board.placeStone(stone)
    checkIllegalMove(updatedBoard.stones.contains(stone)) {
        "It is suicide to place a stone at the point (${point.x}; ${point.y})."
    }

    return addProperty(SgfProperty.Move.Stone(color, Move.Stone(point)))
}

private fun SgfGameTree.isBoardRepeating(stone: Stone, size: Int): Boolean {
    if (sequence.isSingle()) {
        return false
    }

    val previousPosition = SgfGameTree(sequence.dropLast(1).toNelUnsafe(), trees).toBoard(size)
    val nextPosition = toBoard(size).placeStone(stone)

    return previousPosition.stones.toSet() == nextPosition.stones.toSet()
}

fun SgfGameTree.toBoard(size: Int): Board = toBoard(stones, size)

private fun toBoard(stones: List<Stone>, size: Int) = stones.fold(Board.empty(size)) { board, stone ->
    board.placeStone(stone)
}

private fun Board.placeStone(stone: Stone): Board {
    val board = copy(stones = stones + stone)
    val adjacentStones = board.stones.filter { it.point in stone.adjacentPoints(size) && it.color != stone.color }

    return adjacentStones
        .fold(board) { acc, s -> acc.removeConnectedStonesIfTheyAreDead(s) }
        .removeConnectedStonesIfTheyAreDead(stone)
}

private val Color.inverse
    get() = when (this) {
        Color.Black -> Color.White
        Color.White -> Color.Black
    }

private fun Board.removeConnectedStonesIfTheyAreDead(stone: Stone): Board {
    val group = getGroupContainingStone(stone)
    val liberties = countLibertiesForGroup(group)

    return if (liberties <= 0) {
        removeGroup(group).increaseCaptureCount(stone.color.inverse, group.size)
    } else {
        this
    }
}

private fun Board.removeGroup(group: Set<Stone>) = copy(stones = stones - group)

private fun Board.increaseCaptureCount(color: Color, numberOfCaptures: Int) = when (color) {
    Color.Black -> copy(blackCaptures = blackCaptures + numberOfCaptures)
    Color.White -> copy(whiteCaptures = whiteCaptures + numberOfCaptures)
}

private fun Board.getGroupContainingStone(stone: Stone): Set<Stone> {
    fun buildGroup(group: Set<Stone>, stone: Stone): Set<Stone> {
        val adjacentStones = stones
            .filter {
                it.point in stone.adjacentPoints(size) && it !in group && it.color == stone.color
            }

        return adjacentStones.fold(group + adjacentStones, ::buildGroup)
    }

    return buildGroup(setOf(stone), stone)
}

private fun Board.countLibertiesForGroup(group: Set<Stone>): Int = group.sumOf { stone ->
    val adjacentPoints = stone.adjacentPoints(size)
    val totalPossibleLiberties = adjacentPoints.size
    val enemies = stones.count { it.point in adjacentPoints && it.color != stone.color }
    val deadLiberties = group.count { it.point in adjacentPoints }

    totalPossibleLiberties - enemies - deadLiberties
}

private fun checkIllegalMove(predicate: Boolean, message: () -> String) {
    if (!predicate) {
        throw IllegalMoveException(message())
    }
}

fun newGame(
    blackPlayer: Player,
    whitePlayer: Player,
    boardSize: Int,
    komi: Double,
    handicap: Int
): SgfGameTree {
    require(handicap in legalHandicapSizesForBoardSize(boardSize)) {
        "Illegal handicap"
    }

    require(boardSize in listOf(9, 13, 19)) {
        "Illegal board size"
    }

    val properties = setOf(
        SgfProperty.Root.BoardSize(boardSize),
        SgfProperty.Root.Komi(komi),
        SgfProperty.GameInfo.BlackPlayerName(simpleTextOf(blackPlayer.name)),
        SgfProperty.GameInfo.WhitePlayerName(simpleTextOf(whitePlayer.name))
    )

    val handicapProperties = if (handicap >= 2) {
        setOf(
            SgfProperty.GameInfo.Handicap(handicap),
            SgfProperty.Setup.Position(Color.Black, handicapPoints(boardSize, handicap))
        )
    } else {
        emptySet()
    }

    return SgfGameTree(
        sequence = nelOf(SgfNode(properties + handicapProperties)),
        trees = emptyList()
    )
}

private fun edgeDistanceForBoardSize(boardSize: Int) = when {
    boardSize < 7 -> null
    boardSize < 13 -> 3
    else -> 4
}

private fun tengen(boardSize: Int) = Point(
    middle(boardSize),
    middle(boardSize)
)

private fun middle(n: Int) = ceil(n / 2f).toInt()

private fun handicapPoints(boardSize: Int, handicap: Int): Set<Point> {
    val edgeDistance = edgeDistanceForBoardSize(boardSize)

    return if (edgeDistance == null) {
        emptySet()
    } else {
        fun points(handicap: Int): Set<Point> = when (handicap) {
            2 -> setOf(
                Point(edgeDistance, boardSize - edgeDistance + 1),
                Point(boardSize - edgeDistance + 1, edgeDistance)
            )
            3 -> setOf(
                Point(boardSize - edgeDistance + 1, boardSize - edgeDistance + 1)
            ) + points(2)
            4 -> setOf(Point(edgeDistance, edgeDistance)) + points(3)
            5 -> setOf(tengen(boardSize)) + points(4)
            6 -> setOf(
                Point(edgeDistance, middle(boardSize)),
                Point(boardSize - edgeDistance + 1, middle(boardSize))
            ) + points(4)
            7 -> setOf(tengen(boardSize)) + points(6)
            8 -> setOf(
                Point(middle(boardSize), edgeDistance),
                Point(middle(boardSize), boardSize - edgeDistance + 1)
            ) + points(6)
            9 -> setOf(tengen(boardSize)) + points(8)
            else -> emptySet()
        }

        points(handicap)
    }
}

private fun legalHandicapSizesForBoardSize(boardSize: Int): IntRange = when {
    boardSize < 7 -> 0..0
    boardSize == 7 -> 0..4
    boardSize % 2 == 0 -> 0..4
    else -> 0..9
}

val SgfGameTree.boardSizeOrNull: Int?
    get() = rootNode().findProperty<SgfProperty.Root.BoardSize>()?.size

val SgfGameTree.boardSizeUnsafe
    get() = checkNotNull(boardSizeOrNull) {
        "The game tree contains no information about board size."
    }

val SgfGameTree.komiOrNull: Double?
    get() = rootNode().findProperty<SgfProperty.Root.Komi>()?.komi

val SgfGameTree.komiUnsafe: Double
    get() = checkNotNull(komiOrNull) {
        "The game tree contains no information about komi."
    }

val SgfGameTree.handicap: Int
    get() = rootNode().findProperty<SgfProperty.GameInfo.Handicap>()?.numberOfStones ?: 0

val SgfGameTree.asBoardOrNull
    get() = boardSizeOrNull?.let { toBoard(it) }

val SgfGameTree.asBoardUnsafe
    get() = checkNotNull(asBoardOrNull) {
        "Can't construct a board with this game tree."
    }

val SgfGameTree.blackPlayerNameOrNull
    get() = rootNode().findProperty<SgfProperty.GameInfo.BlackPlayerName>()?.name

val SgfGameTree.blackPlayerNameUnsafe
    get() = checkNotNull(blackPlayerNameOrNull) {
        "There's no information about the name of the black player."
    }

val SgfGameTree.whitePlayerNameOrNull
    get() = rootNode().findProperty<SgfProperty.GameInfo.WhitePlayerName>()?.name

val SgfGameTree.whitePlayerNameUnsafe
    get() = checkNotNull(whitePlayerNameOrNull) {
        "There's no information about the name of the white player."
    }