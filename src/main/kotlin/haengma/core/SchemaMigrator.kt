package haengma.core

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.output.MigrateResult


data class SchemaMigrationConfig(
    val url: String,
    val userName: String,
    val password: String,
    val schema: String,
    val migrationsLocation: Location
)

class SchemaMigrator(config: SchemaMigrationConfig) {
    private val flyway = Flyway.configure()
        .dataSource(config.url, config.userName, config.password)
        .schemas(config.schema)
        .defaultSchema(config.schema)
        .locations(config.migrationsLocation)
        .load()

    fun migrate(): MigrateResult = flyway.migrate()
}