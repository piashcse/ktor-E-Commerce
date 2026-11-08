package com.piashcse.database

import com.piashcse.config.DotEnvConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

private lateinit var hikariDataSource: HikariDataSource
private var readHikariDataSource: HikariDataSource? = null
private var readDatabase: Database? = null

internal fun getHikariDataSource(): HikariDataSource = hikariDataSource

/** Optional read-only Exposed database for replica routing. */
internal fun getReadDatabase(): Database? = readDatabase

fun closeDataSource() {
    if (::hikariDataSource.isInitialized && !hikariDataSource.isClosed) hikariDataSource.close()
    readHikariDataSource?.takeIf { !it.isClosed }?.close()
}

fun configureDatabase() {
    hikariDataSource = createDataSource()
    val flyway =
        Flyway.configure()
            .dataSource(hikariDataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            // outOfOrder(false) is intentional: applied migrations are immutable; out-of-order
            // runs stay disabled so a stray older migration fails fast instead of running late.
            .validateOnMigrate(true)
            .load()
    // migrate() validates already-applied migrations itself; calling validate()
    // first fails on a fresh database because no history exists yet.
    flyway.migrate()
    Database.connect(hikariDataSource)

    DotEnvConfig.dbReadUrl?.takeIf { it.isNotBlank() }?.let { readUrl ->
        val readDs =
            HikariDataSource(
                HikariConfig().apply {
                    driverClassName = "org.postgresql.Driver"
                    jdbcUrl = readUrl
                    username = DotEnvConfig.dbUser
                    password = DotEnvConfig.dbPassword
                    maximumPoolSize = DotEnvConfig.dbPoolSize
                    minimumIdle = DotEnvConfig.dbMinIdle
                    idleTimeout = 30_000
                    maxLifetime = 600_000
                    connectionTimeout = DotEnvConfig.dbConnectionTimeoutMs
                    isReadOnly = true
                    validate()
                },
            )
        readHikariDataSource = readDs
        readDatabase = Database.connect(readDs)
    }
}

private fun createDataSource() =
    HikariDataSource(
        HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://${DotEnvConfig.dbHost}:${DotEnvConfig.dbPort}/${DotEnvConfig.dbName}"
            username = DotEnvConfig.dbUser
            password = DotEnvConfig.dbPassword
            maximumPoolSize = DotEnvConfig.dbPoolSize
            minimumIdle = DotEnvConfig.dbMinIdle
            idleTimeout = 30_000
            maxLifetime = 600_000
            connectionTimeout = DotEnvConfig.dbConnectionTimeoutMs
            leakDetectionThreshold = 60_000
            validate()
        },
    )
