package haengma.core.logics

import haengma.core.game.*
import haengma.core.models.PlayerId
import haengma.core.sgf.models.Move
import haengma.core.sgf.models.SgfGameTree
import haengma.core.sgf.models.textOf

object GameLogicContext

fun GameLogicContext.playMove(
    state: GameState,
    playerId: PlayerId,
    move: Move
): GameState {
    val color = state.getPlayerColorByIdOrThrow(playerId)

    val tree = state.gameTree.addMove(color, move, state.gameTree.boardSizeUnsafe)

    return state.copy(
        gameTree = tree
    )
}

fun GameLogicContext.createGame(
    black: Player,
    white: Player,
    boardSize: Int,
    handicap: Int,
    komi: Double
): GameState = GameState(
    black,
    white,
    newGame(black, white, boardSize, komi, handicap)
)

fun GameLogicContext.resign(
    state: GameState,
    playerId: PlayerId
) = state.copy(
    gameTree = state.gameTree.resign(
        state.getPlayerColorByIdOrThrow(playerId)
    )
)

fun GameLogicContext.comment(
    state: GameState,
    playerId: PlayerId,
    comment: String
): GameState {
    val playerName = state.getPlayerNameByIdOrThrow(playerId)
    val text = "[$playerName]: $comment"

    return state.copy(
        gameTree = state.gameTree.comment(textOf(text))
    )
}