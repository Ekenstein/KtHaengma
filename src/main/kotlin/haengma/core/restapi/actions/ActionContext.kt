package haengma.core.restapi.actions

import haengma.core.services.ServiceContext
import java.time.Clock

data class ActionContext(
    val services: ServiceContext,
    val clock: Clock,
    val connections: Set<Connection>
)

