package haengma.core.restapi.routes

import haengma.core.restapi.actions.ActionContext
import haengma.core.restapi.actions.addMove
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*

fun Route.gameRoutes(actionContext: ActionContext) = webSocket("/game") {
    send("You are connected")
    actionContext.addMove(this)
}