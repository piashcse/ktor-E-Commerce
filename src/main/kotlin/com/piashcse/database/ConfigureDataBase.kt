package com.piashcse.database

import com.piashcse.config.DotEnvConfig
import com.piashcse.database.entities.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.Slf4jSqlDebugLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import javax.sql.DataSource


fun configureDataBase() {
    initDB()
    transaction {
        TransactionManager.current().addLogger(Slf4jSqlDebugLogger)
        SchemaUtils.create(
            UserTable,
            UserProfileTable,
            ShopTable,
            ShopCategoryTable,
            ProductTable,
            ReviewRatingTable,
            ProductCategoryTable,
            ProductSubCategoryTable,
            BrandTable,
            CartItemTable,
            OrderTable,
            OrderItemTable,
            WishListTable,
            ShippingTable,
            PaymentTable,
            PolicyDocumentTable,
            PolicyConsentTable
        )
    }
}

private fun initDB() {
    // Create HikariConfig with values from DotEnv
    // we can also define from hikari.properties like HikariDataSource(HikariConfig("/hikari.properties"))
    val config = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = "jdbc:postgresql://${DotEnvConfig.dbHost}:${DotEnvConfig.dbPort}/${DotEnvConfig.dbName}"
        username = DotEnvConfig.dbUser
        password = DotEnvConfig.dbPassword
    }
    
    HikariDataSource(config).also { dataSource ->
        runFlyway(dataSource)
        Database.connect(dataSource)
    }
}

private fun runFlyway(datasource: DataSource) {
    val flyway = Flyway.configure().dataSource(datasource).load()
    try {
        flyway.info()
        flyway.migrate()
    } catch (e: Exception) {
        throw e
    }
}