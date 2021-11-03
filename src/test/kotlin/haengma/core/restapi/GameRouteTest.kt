package haengma.core.restapi

import io.ktor.application.*
import io.ktor.server.testing.*
import org.junit.Test

class GameRouteTest {
    @Test
    fun test() {
        withTestApplication(Application::module) {
            handleWebSocketConversation("/game") { incoming, outgoing ->
                
            }
        }
    }
}