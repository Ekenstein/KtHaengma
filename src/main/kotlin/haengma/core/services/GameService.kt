package haengma.core.services

import haengma.core.game.GameState
import haengma.core.logics.comment
import haengma.core.logics.playMove
import haengma.core.logics.resign
import haengma.core.models.PlayerId
import haengma.core.sgf.models.Move

fun ServiceContext.addMove(state: GameState, playerId: PlayerId, move: Move) = logics.games.playMove(
    state,
    playerId,
    move
)

fun ServiceContext.resign(state: GameState, playerId: PlayerId) = logics.games.resign(state, playerId)

fun ServiceContext.comment(state: GameState, playerId: PlayerId, comment: String) = logics.games.comment(
    state,
    playerId,
    comment
)