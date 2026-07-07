package com.piashcse.database

import com.piashcse.config.DotEnvConfig
import com.piashcse.database.entities.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.core.Slf4jSqlDebugLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val allTables = arrayOf(
    UserTable, UserProfileTable, ShopTable, ShopCategoryTable,
    ProductTable, ProductImageTable, ReviewRatingTable, ProductCategoryTable,
    ProductSubCategoryTable, BrandTable, CartItemTable,
    OrderTable, OrderItemTable, WishListTable, ShippingAddressTable,
    ShippingMethodTable, PaymentTable, PolicyDocumentTable,
    PolicyConsentTable, InventoryTable, SellerTable, RefreshTokenTable,
    LoginAttemptTable, BlacklistedTokenTable, RefundRequestTable,
    OrderStatusHistoryTable,
    AuditLogTable,
)

fun configureDataBase() {
    val dataSource = createDataSource()
    Database.connect(dataSource)
    createTables()
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

private fun createTables() {
    val isDev = System.getenv("KTOR_DEVELOPMENT")?.toBoolean() == true
    transaction {
        if (isDev) {
            TransactionManager.current().addLogger(Slf4jSqlDebugLogger)
            SchemaUtils.create(*allTables)
        } else {
            SchemaUtils.createMissingTablesAndColumns(*allTables)
        }
    }
}
