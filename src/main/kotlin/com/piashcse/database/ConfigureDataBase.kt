package com.piashcse.database

import com.piashcse.entities.*
import com.piashcse.entities.OrderItemTable
import com.piashcse.entities.OrderTable
import com.piashcse.entities.BrandTable
import com.piashcse.entities.ProductImageTable
import com.piashcse.entities.ProductTable
import com.piashcse.entities.ProductCategoryTable
import com.piashcse.entities.ProductSubCategoryTable
import com.piashcse.entities.ShopCategoryTable
import com.piashcse.entities.ShopTable
import com.piashcse.entities.UserProfileTable
import com.piashcse.entities.UserTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
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
            ReviewRatingTable,
            ProductImageTable,
            ProductCategoryTable,
            ProductSubCategoryTable,
            BrandTable,
            CartItemTable,
            OrderTable,
            OrderItemTable,
            WishListTable,
            ShippingTable,
            PaymentTable
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

private fun runFlyway(datasource: DataSource) {
    val flyway = Flyway.configure().dataSource(datasource).load()
    try {
        flyway.info()
        flyway.migrate()
    } catch (e: Exception) {
        throw e
    }
}