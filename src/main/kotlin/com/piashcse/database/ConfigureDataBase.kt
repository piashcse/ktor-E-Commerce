package com.piashcse.database

import com.piashcse.entities.ShippingTable
import com.piashcse.entities.WishListTable
import com.piashcse.entities.orders.CartItemTable
import com.piashcse.entities.orders.OrderItemTable
import com.piashcse.entities.orders.OrderTable
import com.piashcse.entities.product.BrandTable
import com.piashcse.entities.product.ProductImageTable
import com.piashcse.entities.product.ProductTable
import com.piashcse.entities.product.category.ProductCategoryTable
import com.piashcse.entities.product.category.ProductSubCategoryTable
import com.piashcse.entities.shop.ShopCategoryTable
import com.piashcse.entities.shop.ShopTable
import com.piashcse.entities.user.UserProfileTable
import com.piashcse.entities.user.UserTable
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
            ProductImageTable,
            ProductCategoryTable,
            ProductSubCategoryTable,
            BrandTable,
            CartItemTable,
            OrderTable,
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

private fun runFlyway(datasource: DataSource) {
    val flyway = Flyway.configure().dataSource(datasource).load()
    try {
        flyway.info()
        flyway.migrate()
    } catch (e: Exception) {
        throw e
    }
}