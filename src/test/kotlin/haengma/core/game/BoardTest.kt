package haengma.core.game

import haengma.core.models.PlayerId
import haengma.core.sgf.models.Color
import haengma.core.sgf.models.move
import haengma.core.sgf.models.simpleTextOf
import haengma.core.sgf.parseSgf
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import utils.assertSingle
import utils.sgf
import java.util.*


class BoardTest {
    private val blackPlayer = Player(PlayerId(UUID.randomUUID().toString()), "Sakata Eio")
    private val whitePlayer = Player(PlayerId(UUID.randomUUID().toString()), "Go Seigen")

    @Test
    fun `creating new game will setup board with handicap points if necessary`() {
        val tree = newGame(blackPlayer, whitePlayer, 19, 0.0, 9)
        assertEquals(9, tree.handicap)
        assertEquals(19, tree.boardSizeUnsafe)
        assertEquals(0.0, tree.komiUnsafe, 0.0)
        assertEquals(simpleTextOf(blackPlayer.name), tree.blackPlayerNameUnsafe)
        assertEquals(simpleTextOf(whitePlayer.name), tree.whitePlayerNameUnsafe)
        assertEquals(tree.handicap, tree.asBoardUnsafe.stones.size)
    }

    @Test
    fun `verify a capture in a semeai`() {
        val sgf = "(;AW[cn][bo][ap][bp][aq][cq][dq][ar][dr][er][as][ds]" +
                "AB[cp][dp][ep][bq][eq][fq][gq][br][cr][fr][bs][es][fs])"
        val tree = assertSingle(parseSgf(sgf))

        assertAll(
            {
                val board = tree.addMove(Color.Black, move(3, 19), 19).toBoard(19)
                assertEquals(5, board.blackCaptures)
            },
            {
                val board = tree.addMove(Color.White, move(3, 19), 19).toBoard(19)
                assertEquals(4, board.whiteCaptures)
            }
        )
    }

    @Test
    fun `can not play a point that would result in a suicide`() {
        val sgf = "(;AW[cd][dd][de]AB[cc][dc][bd][ed][be][ee][cf][df])"
        val tree = assertSingle(parseSgf(sgf))
        assertThrows<IllegalMoveException> { tree.addMove(Color.White, move(3, 5), 19) }
    }

    @Test
    fun `can not play on an occupied point`() {
        val tree = sgf {
            tree {
                node {
                    blackMove(move(3, 3))
                }
            }
        }.single()

        assertAll(
            { assertThrows<IllegalMoveException> { tree.addMove(Color.Black, move(3, 3), 19) } },
            { assertThrows<IllegalMoveException> { tree.addMove(Color.White, move(3, 3), 19) } }
        )
    }

    @Test
    fun `re-taking a ko directly will throw illegal move exception`() {
        val sgf = "(;B[gd];W[hd];B[he];W[ie];B[hc];W[ic];B[id];W[jd];B[pp];W[hd])"
        val tree = assertSingle(parseSgf(sgf))
        assertThrows<IllegalMoveException> { tree.addMove(Color.Black, move(9, 4), 19) }
    }

    @Test
    fun `passing counts as if the board has been changed in a ko fight`() {
        val sgf = """(;B[jd]
;W[je]
;B[ie]
;W[if]
;B[ke]
;W[kf]
;B[jf]
;W[jg]
;B[])"""
        val tree = assertSingle(parseSgf(sgf))

        val newTree = tree.addMove(Color.White, move(10, 5), 19)
        val board = newTree.toBoard(19)
        assertEquals(1, board.whiteCaptures)
    }

    @Test
    fun `can initiate ko`() {
        val sgf = "(;B[ab];W[bb];B[bc];W[cc];B[ba];W[ca];B[cb];W[db])"
        val tree = assertSingle(parseSgf(sgf))

        val newTree = tree.addMove(Color.White, move(2, 2), 19)

        val board = newTree.toBoard(19)
        assertEquals(1, board.whiteCaptures)
        assertThrows<IllegalMoveException> { tree.addMove(Color.Black, move(3, 3), 19) }
    }
}