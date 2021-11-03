package haengma.core.restapi.actions

import haengma.core.game.GameState
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

suspend fun ActionContext.addMove(session: WebSocketServerSession) {
    for (frame in session.incoming) {
        val state = when (frame) {
            is Frame.Text -> Json.decodeFromString<GameState>(frame.readText())
            is Frame.Binary,
            is Frame.Close,
            is Frame.Ping,
            is Frame.Pong -> TODO()
        }


    }
}