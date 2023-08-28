package com.piashcse.dbhelper

import com.piashcse.entities.ShippingTable
import com.piashcse.entities.product.category.CategoryTable
import com.piashcse.entities.product.category.SubCategoryTable
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
import org.slf4j.LoggerFactory
import java.net.URI
import javax.sql.DataSource

object DatabaseFactory {
    private val log = LoggerFactory.getLogger(this::class.java)
    fun init() {
        initDB()
        //Database.connect(hikari())
        transaction {
            // print sql to std-out
            addLogger(StdOutSqlLogger)
            create(
                UserTable,
                UserProfileTable,
                ShopTable,
                ShopCategoryTable,
                ProductTable,
                CategoryTable,
                SubCategoryTable,
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


    // database connection for h2
    private fun hikariForH2(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.h2.Driver"
        config.jdbcUrl = "jdbc:h2:file:~/documents/db/h2db"
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    private fun runFlyway(datasource: DataSource) {
        val flyway = Flyway.configure().dataSource(datasource).load()
        try {
            flyway.info()
            flyway.migrate()
        } catch (e: Exception) {
            log.error("Exception running flyway migration", e)
            throw e
        }
        log.info("Flyway migration has finished")
    }
}