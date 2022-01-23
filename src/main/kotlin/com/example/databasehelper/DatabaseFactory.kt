package com.example.databasehelper

import com.example.entities.product.*
import com.example.entities.product.defaultproductcategory.ProductCategoryTable
import com.example.entities.product.defaultproductcategory.ProductSubCategoryTable
import com.example.entities.product.defaultvariant.ProductColorTable
import com.example.entities.product.defaultvariant.ProductSizeTable
import com.example.entities.shop.ShopCategoryTable
import com.example.entities.shop.ShopTable
import com.example.entities.user.UserHasTypeTable
import com.example.entities.user.UserTypeTable
import com.example.entities.user.UserProfileTable
import com.example.entities.user.UserTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object DatabaseFactory {
    private val log = LoggerFactory.getLogger(this::class.java)
    fun init() {
        initDB()
        transaction {
            create(UserTable, UserProfileTable, UserTypeTable, UserHasTypeTable,ShopTable, ShopCategoryTable, ProductCategoryTable, ProductSubCategoryTable, ProductTable, ProductSizeTable, ProductColorTable)
            //createMissingTablesAndColumns(UsersTable, UsersProfileTable, UserTypeTable, UserHasTypeTable, ShopTable, ShopCategoryTable, ProductCategoryTable, ProductSubCategoryTable, ProductTable, ProductSizeTable, ProductColorTable)
        }
    }

    private fun initDB() {
        // database connection is handled from hikari properties
        val config = HikariConfig("/hikari.properties")
        val ds = HikariDataSource(config)
        runFlyway(ds)
        Database.connect(ds)
    }

    // database connection for h2
    private fun hikari(): HikariDataSource {
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