package haengma.core.restapi

import haengma.core.logics.LogicContext
import haengma.core.restapi.actions.ActionContext
import haengma.core.restapi.actions.Connection
import haengma.core.restapi.routes.gameRoutes
import haengma.core.services.ServiceContext
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import java.time.Clock
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

class Server private constructor(private val engine: ApplicationEngine) {
    companion object {
        fun start(): Server {
            val engine = embeddedServer(Netty, port = 8080) {
                install(WebSockets) {
                    pingPeriod = Duration.ofSeconds(15)
                    timeout = Duration.ofSeconds(15)
                    maxFrameSize = Long.MAX_VALUE
                    masking = false
                }

                module()

            }.start(wait = true)

            return Server(engine)
        }
    }

    fun stop() = engine.stop(1000, 1000)
}

fun Application.module(testing: Boolean = false) {
    val logics = LogicContext()
    val services = ServiceContext(logics)
    val connections = Collections.synchronizedSet<Connection>(LinkedHashSet())
    val actions = ActionContext(services, Clock.systemUTC(), connections)

    routing {
        gameRoutes(actions)
    }
}