package haengma.core.sgf

import haengma.core.sgf.models.*
import haengma.core.utils.nelOf
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import utils.*

class SgfParserTest {
    @Test
    fun `white move with no coordinates parses to pass`() {
        val tree = parseSgf("(;W[])")
        val expectedTree = sgf {
            tree {
                node {
                    whiteMove(pass)
                }
            }
        }

        assertEquals(expectedTree, tree)
    }

    @Test
    fun `black move with no coordinates parses to pass`() {
        val tree = parseSgf("(;B[])")
        val expectedTree = sgf {
            tree {
                node {
                    blackMove(pass)
                }
            }
        }
        assertEquals(expectedTree, tree)
    }

    @Test
    fun `white move with coordinate parses to move`() {
        val tree = parseSgf("(;W[aa])")
        val expectedTree = sgf {
            tree {
                node {
                    whiteMove(move(1, 1))
                }
            }
        }
        assertEquals(expectedTree, tree)
    }

    @Test
    fun `black move with coordinate parses to move`() {
        val tree = assertSingle(parseSgf("(;B[aa])"))
        assertEquals(sgfFactory.blackMove(move(1, 1)).asGameTree(), tree)
    }

    @Test
    fun `SZ parses to board size`() {
        val tree = assertSingle(parseSgf("(;SZ[19])"))
        assertEquals(sgfFactory.boardSize(19).asGameTree(), tree)
    }

    @Test
    fun `KM parses to komi`() {
        val tree = assertSingle(parseSgf("(;KM[6.5])"))
        assertEquals(sgfFactory.komi(6.5).asGameTree(), tree)
    }

    @Test
    fun `AB parses to black position`() {
        val tree = assertSingle(parseSgf("(;AB[aa][bb][cc])"))
        val expectedTree = sgfFactory.blackPosition(
            Point(1, 1),
            Point(2, 2),
            Point(3, 3)
        ).asGameTree()

        assertEquals(expectedTree, tree)
    }

    @Test
    fun `AW parses to white position`() {
        val tree = assertSingle(parseSgf("(;AW[aa][bb][cc])"))
        val expectedTree = sgfFactory.whitePosition(
            Point(1, 1),
            Point(2, 2),
            Point(3, 3)
        ).asGameTree()

        assertEquals(expectedTree, tree)
    }

    @Test
    fun `AW with compressed points to white position`() {
        val tree = parseSgf("(;AW[aa:ab][ba:bb])")
        val expectedTree = sgf {
            tree {
                node {
                    whitePosition(
                        setOf(
                            Point(1, 1),
                            Point(1, 2),
                            Point(2, 1),
                            Point(2, 2)
                        )
                    )
                }
            }
        }

        assertEquals(expectedTree, tree)
    }

    @Test
    fun `HA parses to handicap`() {
        val tree = parseSgf("(;HA[9])")
        val expectedTree = sgf {
            tree {
                node {
                    handicap(9)
                }
            }
        }

        assertEquals(expectedTree, tree)
    }

    @Test
    fun `PB parses to black player name`() {
        val tree = parseSgf("(;PB[Test])")
        val expectedTree = sgf {
            tree {
                node {
                    blackPlayerName(simpleTextOf("Test"))
                }
            }
        }

        assertEquals(expectedTree, tree)
    }

    @Test
    fun `PW parses to white player name`() {
        val tree = parseSgf("(;PW[Test])")
        val expectedTree = sgf {
            tree {
                node {
                    whitePlayerName(simpleTextOf("Test"))
                }
            }
        }

        assertEquals(expectedTree, tree)
    }

    @Test
    fun `C parses to comment`() {
        val comment = """Meijin NR: yeah, k4 is won\
derful
sweat NR: thank you! :\)
dada NR: yup. I like this move too. It's a move only to be expected from a pro. I really like it :)
jansteen 4d: Can anyone\
 explain [me\] k4?"""

        val expectedComment = """Meijin NR: yeah, k4 is wonderful
sweat NR: thank you! :)
dada NR: yup. I like this move too. It's a move only to be expected from a pro. I really like it :)
jansteen 4d: Can anyone explain [me] k4?"""

        val tree = assertSingle(parseSgf("(;C[$comment])"))
        val expectedTree = sgfFactory.comment(textOf(expectedComment)).asGameTree()
        assertEquals(expectedTree, tree)
    }

    @Test
    fun `having multiple trees results in multiple items in the collection`() {
        val sgf = "(;B[aa])(;W[])"
        val result = parseSgf(sgf)
        val collection = assertNotEmpty(result)
        assertEquals(2, collection.size)

        val expectedFirstTree = SgfGameTree(
            sequence = nelOf(SgfProperty.Move.Stone(Color.Black, move(1, 1)).asNode),
            trees = emptyList()
        )

        val expectedSecondTree = SgfGameTree(
            sequence = nelOf(SgfProperty.Move.Stone(Color.White, pass).asNode),
            trees = emptyList()
        )

        assertEquals(expectedFirstTree, collection.head)
        assertEquals(expectedSecondTree, collection.tail.single())
    }
}