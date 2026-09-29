package com.piashcse.database

import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ShippingMethodDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WishListDAO
import com.piashcse.database.entities.WishListTable
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assume
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Real-Postgres integration test (Testcontainers): validates every Flyway
 * migration V1..V15 + seed, then a vertical slice user → shop → product →
 * inventory → wishlist, including the V9 uniqueness guard. Skipped when no
 * Docker daemon is available.
 */
class PostgresIntegrationTest {
    private fun dockerAvailable(): Boolean =
        runCatching {
            ProcessBuilder("docker", "info").redirectErrorStream(true).start().waitFor() == 0
        }.getOrDefault(false)

    @Test
    fun `migrations apply and vertical slice persists`() {
        Assume.assumeTrue("Docker daemon required", dockerAvailable())
        val pg = PostgreSQLContainer("postgres:16-alpine")
        pg.start()
        try {
            val flyway =
                Flyway.configure()
                    .dataSource(pg.jdbcUrl, pg.username, pg.password)
                    .locations("classpath:db/migration")
                    .load()
            val applied = flyway.migrate()
            assertTrue(applied.migrations.size >= 15, "Expected V1..V15, got ${applied.migrations.size}")

            Database.connect(pg.jdbcUrl, user = pg.username, password = pg.password)
            lateinit var sliceUserId: org.jetbrains.exposed.v1.core.dao.id.EntityID<String>
            lateinit var sliceProductId: org.jetbrains.exposed.v1.core.dao.id.EntityID<String>
            transaction {
                // R__ seeds loaded.
                assertTrue(ShippingMethodDAO.all().count() >= 3, "Seed shipping methods missing")
                assertTrue(ProductCategoryDAO.all().count() >= 4, "Seed categories missing")

                val user =
                    UserDAO.new {
                        email = "slice@example.com"
                        userType = UserType.CUSTOMER
                        password = "hashed"
                        isVerified = true
                    }
                val shopCategory = ShopCategoryDAO.new { name = "Slice Shops" }
                val shop =
                    ShopDAO.new {
                        userId = user.id
                        categoryId = shopCategory.id
                        name = "Slice Shop"
                        status = ShopStatus.APPROVED
                    }
                val category = ProductCategoryDAO.find { ProductCategoryTable.name eq "Electronics" }.first()
                val product =
                    ProductDAO.new {
                        userId = user.id
                        shopId = shop.id
                        categoryId = category.id
                        name = "Slice Phone"
                        description = "Integration slice phone"
                        sku = "SLICE-001"
                        price = BigDecimal("99.99")
                    }
                InventoryDAO.new {
                    productId = product.id
                    shopId = shop.id
                    stockQuantity = 10
                }

                WishListDAO.new {
                    userId = user.id
                    productId = product.id
                }
                sliceUserId = user.id
                sliceProductId = product.id
            }
            // V9 uniqueness is a COMMIT-time guard here (writes flush at commit):
            // the duplicate must fail in its own transaction, never persist.
            assertFailsWith<Exception> {
                transaction {
                    WishListDAO.new {
                        userId = sliceUserId
                        productId = sliceProductId
                    }
                }
            }
            transaction {
                assertTrue(
                    WishListDAO.find { WishListTable.userId eq sliceUserId }.count() == 1L,
                    "Wishlist slice missing",
                )
                assertTrue(ProductDAO.findById(sliceProductId.value) != null, "Product reread failed")
            }
        } finally {
            pg.stop()
        }
    }
}
