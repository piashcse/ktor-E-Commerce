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
import org.jetbrains.exposed.v1.core.dao.id.EntityID
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
 * Real-Postgres integration test.
 *
 * Prefers an externally provided database (`DB_HOST`/`DB_PORT`/`DB_NAME`/
 * `DB_USER`/`DB_PASSWORD`, as configured by the CI Postgres service) and falls
 * back to Testcontainers locally. It validates every Flyway migration V1..V27,
 * the seed data, the new hardening columns, and a vertical slice
 * user → shop → product → inventory → wishlist.
 */
class PostgresIntegrationTest {
    private fun envDb(): Triple<String, String, String>? {
        val host = System.getenv("DB_HOST")?.takeIf { it.isNotBlank() } ?: return null
        val port = System.getenv("DB_PORT") ?: "5432"
        val name = System.getenv("DB_NAME") ?: "ktor-ecommerce"
        val user = System.getenv("DB_USER") ?: "postgres"
        val password = System.getenv("DB_PASSWORD") ?: "postgres"
        return Triple("jdbc:postgresql://$host:$port/$name", user, password)
    }

    private fun dockerAvailable(): Boolean =
        runCatching {
            ProcessBuilder("docker", "info").redirectErrorStream(true).start().waitFor() == 0
        }.getOrDefault(false)

    private fun columnExists(
        table: String,
        column: String,
    ): Boolean =
        transaction {
            exec(
                "SELECT 1 FROM information_schema.columns WHERE table_name = '$table' AND column_name = '$column'",
            ) { rs -> rs.next() }.also { it }
        } ?: false

    @Test
    fun `migrations apply and vertical slice persists`() {
        val external = envDb()
        if (external != null) {
            runSlice(external.first, external.second, external.third)
            return
        }

        Assume.assumeTrue("Docker daemon or DB_* environment required", dockerAvailable())
        val pg = PostgreSQLContainer("postgres:16-alpine")
        pg.start()
        try {
            runSlice(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runSlice(
        jdbcUrl: String,
        user: String,
        dbPassword: String,
    ) {
        val flyway =
            Flyway.configure()
                .dataSource(jdbcUrl, user, dbPassword)
                .locations("classpath:db/migration")
                .load()
        val applied = flyway.migrate()
        assertTrue(applied.migrations.size >= 27, "Expected V1..V27, got ${applied.migrations.size}")

        Database.connect(jdbcUrl, user = user, password = dbPassword)
        lateinit var sliceUserId: EntityID<String>
        lateinit var sliceProductId: EntityID<String>
        transaction {
            // New hardening columns exist after V18..V20.
            assertTrue(columnExists("payment", "idempotency_key"), "payment.idempotency_key missing")
            assertTrue(columnExists("order", "fx_rate"), "order.fx_rate missing")
            assertTrue(columnExists("order", "tracking_number"), "order.tracking_number missing")
            assertTrue(columnExists("coupon", "per_user_limit"), "coupon.per_user_limit missing")
            assertTrue(columnExists("payout_request", "idempotency_key"), "payout_request missing")
            assertTrue(columnExists("outbox_dead_letter", "payload"), "outbox_dead_letter missing")
            assertTrue(columnExists("refund_request", "quantity"), "refund_request.quantity missing")
            assertTrue(columnExists("tax_rule", "rate"), "tax_rule missing")
            assertTrue(columnExists("api_key", "key_hash"), "api_key missing")
            assertTrue(columnExists("feature_flag", "enabled"), "feature_flag missing")
            assertTrue(columnExists("user", "is_guest"), "user.is_guest missing")
            assertTrue(columnExists("order", "contact_email"), "order.contact_email missing")
            assertTrue(columnExists("warehouse", "code"), "warehouse missing")
            assertTrue(columnExists("inventory", "warehouse_id"), "inventory.warehouse_id missing")
            assertTrue(columnExists("rma", "resolution"), "rma missing")
            assertTrue(columnExists("seller", "kyc_status"), "seller.kyc_status missing")
            assertTrue(columnExists("notification_preference", "event_type"), "notification_preference missing")

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
            val warehouse =
                com.piashcse.database.entities.WarehouseDAO.new {
                    this.shopId = shop.id
                    name = "Default"
                    code = "DEFAULT"
                }
            InventoryDAO.new {
                productId = product.id
                shopId = shop.id
                warehouseId = warehouse.id
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
    }
}
