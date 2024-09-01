package com.piashcse.database

import com.piashcse.entities.ShippingTable
import com.piashcse.entities.product.category.ProductCategoryTable
import com.piashcse.entities.product.category.ProductSubCategoryTable
import com.piashcse.entities.orders.CartItemTable
import com.piashcse.entities.orders.OrderItemTable
import com.piashcse.entities.orders.OrdersTable
import com.piashcse.entities.product.*
import com.piashcse.entities.shop.ShopCategoryTable
import com.piashcse.entities.shop.ShopTable
import com.piashcse.entities.user.UserProfileTable
import com.piashcse.entities.user.UserTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import java.net.URI
import javax.sql.DataSource


fun configureDataBase() {
    initDB()
    transaction {
        addLogger(StdOutSqlLogger)
        create(
            UserTable,
            UserProfileTable,
            ShopTable,
            ShopCategoryTable,
            ProductTable,
            ProductImageTable,
            ProductCategoryTable,
            ProductSubCategoryTable,
            BrandTable,
            CartItemTable,
            OrdersTable,
            OrderItemTable,
            WishListTable,
            ShippingTable
        )
    }
}

private fun initDB() {
    // database connection is handled from hikari properties
    val config = HikariConfig("/hikari.properties")
    val dataSource = HikariDataSource(config)
    runFlyway(dataSource)
    Database.connect(dataSource)
}

private fun hikari(): HikariDataSource {
    val config = HikariConfig()
    config.driverClassName = System.getenv("JDBC_DRIVER")
    config.jdbcUrl = System.getenv("HEROKU_POSTGRESQL_NAVY_URL")
    config.maximumPoolSize = 3
    config.isAutoCommit = true
    config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    config.validate()
    return HikariDataSource(config)
}

// For heroku deployment
private fun hikariForHeroku(): HikariDataSource {
    val config = HikariConfig()
    config.driverClassName = System.getenv("JDBC_DRIVER")
    config.isAutoCommit = false
    config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"

    val uri = URI(System.getenv("DATABASE_URL"))
    val username = uri.userInfo.split(":").toTypedArray()[0]
    val password = uri.userInfo.split(":").toTypedArray()[1]

    config.jdbcUrl =
        "jdbc:postgresql://" + uri.host + ":" + uri.port + uri.path + "?sslmode=require" + "&user=$username&password=$password"

    config.validate()
    return HikariDataSource(config)
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