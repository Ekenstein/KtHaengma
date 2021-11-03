package haengma.core

import haengma.core.logics.GameLogicContext
import haengma.core.logics.LogicContext
import haengma.core.restapi.Server
import haengma.core.restapi.actions.ActionContext
import haengma.core.restapi.actions.Connection
import haengma.core.restapi.routes.gameRoutes
import haengma.core.services.ServiceContext
import io.ktor.application.*
import io.ktor.routing.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import java.time.Clock
import java.util.*
import javax.sql.DataSource
import kotlin.collections.LinkedHashSet

fun main(args: Array<String>) {
    Server.start()
}