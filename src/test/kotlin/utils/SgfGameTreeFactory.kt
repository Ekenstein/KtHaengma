package utils

import haengma.core.sgf.models.*
import haengma.core.utils.nelOf
import haengma.core.utils.toNelUnsafe

class SgfGameTreeFactory {
    fun blackMove(move: Move) = SgfProperty.Move.Stone(Color.Black, move)
    fun whiteMove(move: Move) = SgfProperty.Move.Stone(Color.White, move)
    fun boardSize(n: Int) = SgfProperty.Root.BoardSize(n)
    fun komi(n: Double) = SgfProperty.Root.Komi(n)
    fun blackPosition(position: Set<Point>) = SgfProperty.Setup.Position(Color.Black, position)
    fun blackPosition(vararg position: Point) = blackPosition(position.toSet())
    fun whitePosition(position: Set<Point>) = SgfProperty.Setup.Position(Color.White, position)
    fun whitePosition(vararg position: Point) = whitePosition(position.toSet())
    fun comment(text: Text) = SgfProperty.NodeAnnotation.Comment(text)
    fun handicap(numberOfStones: Int) = SgfProperty.GameInfo.Handicap(numberOfStones)
    fun blackPlayerName(name: SimpleText) = SgfProperty.GameInfo.BlackPlayerName(name)
    fun whitePlayerName(name: SimpleText) = SgfProperty.GameInfo.WhitePlayerName(name)
}

val sgfFactory: SgfGameTreeFactory by lazy { SgfGameTreeFactory() }

fun SgfProperty.asGameTree() = SgfGameTree(
    sequence = nelOf(asNode),
    trees = emptyList()
)

fun sgf(block: SgfBuilder.() -> Unit): List<SgfGameTree> {
    val builder = SgfBuilderImpl()
    builder.block()
    return builder.trees.map(::toSgfGameTree)
}

private fun toSgfGameTree(tree: Tree): SgfGameTree = SgfGameTree(
    sequence = tree.nodes.map { SgfNode(it.properties) }.toNelUnsafe(),
    trees = tree.trees.map(::toSgfGameTree)
)

private data class Tree(
    val trees: List<Tree>,
    val nodes: List<Node>
)

private data class Node(
    val properties: Set<SgfProperty>
)

private class SgfBuilderImpl : SgfBuilder {
    val trees = mutableListOf<Tree>()

    override fun tree(block: SgfTreeBuilder.() -> Unit) {
        val builder = SgfTreeBuilderImpl()
        builder.block()
        trees.add(builder.tree)
    }
}

private class SgfTreeBuilderImpl : SgfTreeBuilder {
    var tree = Tree(emptyList(), emptyList())

    override fun node(block: SgfNodeBuilder.() -> Unit) {
        val builder = SgfNodeBuilderImpl()
        builder.block()
        tree = tree.copy(
            nodes = tree.nodes + Node(builder.properties)
        )
    }

    override fun tree(block: SgfTreeBuilder.() -> Unit) {
        val builder = SgfTreeBuilderImpl()
        builder.block()
        tree = tree.copy(
            trees = tree.trees + builder.tree
        )
    }

}

private class SgfNodeBuilderImpl : SgfNodeBuilder {
    val properties = mutableSetOf<SgfProperty>()
    override fun blackMove(move: Move) {
        properties.add(sgfFactory.blackMove(move))
    }

    override fun whiteMove(move: Move) {
        properties.add(sgfFactory.whiteMove(move))
    }

    override fun boardSize(size: Int) {
        properties.add(sgfFactory.boardSize(size))
    }

    override fun komi(komi: Double) {
        properties.add(sgfFactory.komi(komi))
    }

    override fun blackPosition(stones: Set<Point>) {
        properties.add(sgfFactory.blackPosition(stones))
    }

    override fun whitePosition(stones: Set<Point>) {
        properties.add(sgfFactory.whitePosition(stones))
    }

    override fun comment(text: Text) {
        properties.add(sgfFactory.comment(text))
    }

    override fun handicap(numberOfStones: Int) {
        properties.add(sgfFactory.handicap(numberOfStones))
    }

    override fun blackPlayerName(name: SimpleText) {
        properties.add(sgfFactory.blackPlayerName(name))
    }

    override fun whitePlayerName(name: SimpleText) {
        properties.add(sgfFactory.whitePlayerName(name))
    }

}

interface SgfBuilder {
    fun tree(block: SgfTreeBuilder.() -> Unit)
}

interface SgfTreeBuilder {
    fun node(block: SgfNodeBuilder.() -> Unit)
    fun tree(block: SgfTreeBuilder.() -> Unit)
}

interface SgfNodeBuilder {
    fun blackMove(move: Move)
    fun whiteMove(move: Move)
    fun boardSize(size: Int)
    fun komi(komi: Double)
    fun blackPosition(stones: Set<Point>)
    fun whitePosition(stones: Set<Point>)
    fun comment(text: Text)
    fun handicap(numberOfStones: Int)
    fun blackPlayerName(name: SimpleText)
    fun whitePlayerName(name: SimpleText)
}