package haengma.core.game

import haengma.core.models.PlayerId
import haengma.core.sgf.models.Color
import haengma.core.sgf.models.SgfGameTree

data class Player(val id: PlayerId, val name: String)

data class GameState(
    val blackPlayer: Player,
    val whitePlayer: Player,
    val gameTree: SgfGameTree
)

fun GameState.getPlayerById(playerId: PlayerId) = when (playerId) {
    blackPlayer.id -> Color.Black to blackPlayer
    whitePlayer.id -> Color.White to whitePlayer
    else -> null
}

fun GameState.getPlayerNameById(playerId: PlayerId) = getPlayerById(playerId)?.let { (_, player) -> player.name }

fun GameState.getPlayerColorById(playerId: PlayerId) = getPlayerById(playerId)?.let { (color, _) -> color }

fun GameState.getPlayerColorByIdOrThrow(playerId: PlayerId) = requireNotNull(getPlayerColorById(playerId)) {
    "The player is not part of this game."
}

fun GameState.getPlayerNameByIdOrThrow(playerId: PlayerId) = requireNotNull(getPlayerNameById(playerId)) {
    "The player is not part of this game."
}