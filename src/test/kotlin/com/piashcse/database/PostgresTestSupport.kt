package com.piashcse.database

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.Assume
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertTrue

/**
 * Single source of truth for database-backed integration tests.
 *
 * Prefers the CI `DB_*` service and otherwise reuses one Testcontainers
 * PostgreSQL instance for the whole JVM, so each test no longer pays the
 * container-start cost. Tests keep their own schema/row unique suffixes, which
 * is the same isolation model used against the shared CI database.
 */
internal object PostgresTestSupport {
    data class Credentials(
        val jdbcUrl: String,
        val user: String,
        val password: String,
    )

    private val sharedContainer: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:16-alpine").apply { start() }
    }

    /** Configured database from the environment, or null when tests should use Docker. */
    fun credentials(): Credentials? {
        val host = System.getenv("DB_HOST")?.takeIf { it.isNotBlank() } ?: return null
        val port = System.getenv("DB_PORT") ?: "5432"
        val name = System.getenv("DB_NAME") ?: "ktor-ecommerce"
        val user = System.getenv("DB_USER") ?: "postgres"
        val password = System.getenv("DB_PASSWORD") ?: "postgres"
        return Credentials("jdbc:postgresql://$host:$port/$name", user, password)
    }

    fun dockerAvailable(): Boolean =
        runCatching {
            ProcessBuilder("docker", "info").redirectErrorStream(true).start().waitFor() == 0
        }.getOrDefault(false)

    /** Runs [block] against `DB_*` when configured, otherwise against the shared container. */
    fun <T> withDatabase(block: (jdbcUrl: String, user: String, password: String) -> T): T =
        withCredentials { block(it.jdbcUrl, it.user, it.password) }

    /**
     * Applies Flyway migrations and connects Exposed before running [block].
     * Use this when a test only needs a ready connection.
     */
    fun <T> withMigratedDatabase(block: () -> T): T =
        withCredentials { credentials ->
            Flyway.configure()
                .dataSource(credentials.jdbcUrl, credentials.user, credentials.password)
                .locations("classpath:db/migration")
                .load()
                .migrate()
            Database.connect(credentials.jdbcUrl, user = credentials.user, password = credentials.password)
            block()
        }

    private fun <T> withCredentials(block: (Credentials) -> T): T {
        credentials()?.let { return block(it) }

        // CI must never silently skip the only real-database check.
        if (System.getenv("CI") != null) {
            assertTrue(dockerAvailable(), "DB_* environment or Docker is required in CI")
        } else {
            Assume.assumeTrue("Docker daemon or DB_* environment required", dockerAvailable())
        }
        val container = sharedContainer
        return block(Credentials(container.jdbcUrl, container.username, container.password))
    }
}
