package com.piashcse.database

import com.piashcse.database.entities.BrandTable
import com.piashcse.database.entities.CartItemTable
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentTable
import com.piashcse.database.entities.PolicyConsentTable
import com.piashcse.database.entities.PolicyDocumentTable
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.ProductSubCategoryTable
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ReviewRatingTable
import com.piashcse.database.entities.ShippingTable
import com.piashcse.database.entities.ShopCategoryTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserProfileTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.WishListTable
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