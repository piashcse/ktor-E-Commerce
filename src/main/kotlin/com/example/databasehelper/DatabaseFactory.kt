package com.example.databasehelper

import com.example.entities.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns

object DatabaseFactory {
    fun init() {
        initDB()
        transaction {
            //create(UsersTable, UserHasTypeTable)
             create(UserTypeTable, UserHasTypeTable, UsersTable, UsersProfileTable)
             create(ProductCategoryTable, ProductSubCategoryTable, ProductTable, ProductSizeTable, ProductColorTable)
        }
    }

    private fun initDB() {
        // database connection is handled from hikari properties
        val config = HikariConfig("/hikari.properties")
        val ds = HikariDataSource(config)
        Database.connect(ds)
    }

    // database connection for h2 d
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
}