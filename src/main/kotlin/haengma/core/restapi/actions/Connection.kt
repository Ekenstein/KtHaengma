package haengma.core.restapi.actions

import io.ktor.websocket.*
import java.util.concurrent.atomic.AtomicInteger

class Connection(val session: WebSocketServerSession) {
    companion object {
        var lastId = AtomicInteger(0)
    }
}