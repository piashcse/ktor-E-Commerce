package com.piashcse.database

import com.piashcse.config.DotEnvConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

private lateinit var hikariDataSource: HikariDataSource

internal fun getHikariDataSource(): HikariDataSource = hikariDataSource

fun configureDatabase() {
    hikariDataSource = createDataSource()
    val flyway = Flyway.configure()
        .dataSource(hikariDataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .load()
    flyway.repair()
    flyway.migrate()
    Database.connect(hikariDataSource)
}

private fun createDataSource() = HikariDataSource(
    HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = "jdbc:postgresql://${DotEnvConfig.dbHost}:${DotEnvConfig.dbPort}/${DotEnvConfig.dbName}"
        username = DotEnvConfig.dbUser
        password = DotEnvConfig.dbPassword
        maximumPoolSize = 10
        minimumIdle = 2
        idleTimeout = 30000
        maxLifetime = 600000
        connectionTimeout = 30000
        validate()
    }
)
