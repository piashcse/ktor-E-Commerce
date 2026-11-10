package com.piashcse.database

import com.piashcse.constants.CouponDiscountType
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.CouponDAO
import com.piashcse.database.entities.CouponUsageDAO
import com.piashcse.database.entities.CouponUsageTable
import com.piashcse.database.entities.InventoryDAO
import com.piashcse.database.entities.InventoryTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PaymentTable
import com.piashcse.database.entities.PayoutRequestDAO
import com.piashcse.database.entities.PayoutRequestTable
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.decrementStock
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assume
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * DB-backed concurrency guard tests. Prefers the CI `DB_*` service and falls
 * back to Testcontainers locally; skipped only when neither is available.
 */
class ConcurrencyIntegrationTest {
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

    private fun withDatabase(block: () -> Unit) {
        val external = envDb()
        if (external != null) {
            setup(external.first, external.second, external.third)
            block()
            return
        }
        // CI must never silently skip the only real-database check.
        if (System.getenv("CI") != null) {
            assertTrue(dockerAvailable(), "DB_* environment or Docker is required in CI")
        } else {
            Assume.assumeTrue("Docker daemon or DB_* environment required", dockerAvailable())
        }
        val pg = PostgreSQLContainer("postgres:16-alpine")
        pg.start()
        try {
            setup(pg.jdbcUrl, pg.username, pg.password)
            block()
        } finally {
            pg.stop()
        }
    }

    private fun setup(
        jdbcUrl: String,
        user: String,
        password: String,
    ) {
        Flyway.configure()
            .dataSource(jdbcUrl, user, password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        Database.connect(jdbcUrl, user = user, password = password)
    }

    private fun runConcurrently(
        count: Int,
        block: () -> Unit,
    ): List<Result<Unit>> {
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(count)
        val futures =
            (1..count).map {
                pool.submit<Result<Unit>> {
                    start.await()
                    runCatching(block)
                }
            }
        start.countDown()
        val results = futures.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()
        return results
    }

    private fun newCategory(): ProductCategoryDAO = ProductCategoryDAO.new { name = "Concurrency-${UUID.randomUUID()}" }

    @Test
    fun `concurrent stock decrements never oversell`() {
        withDatabase {
            lateinit var productId: EntityID<String>
            lateinit var shopId: EntityID<String>
            transaction {
                val user =
                    UserDAO.new {
                        email = "stock-${UUID.randomUUID()}@example.com"
                        userType = UserType.CUSTOMER
                        password = "x"
                    }
                val shopCategory = ShopCategoryDAO.new { name = "Stock Shops ${UUID.randomUUID()}" }
                val shop =
                    ShopDAO.new {
                        userId = user.id
                        categoryId = shopCategory.id
                        name = "Stock Shop"
                        status = ShopStatus.APPROVED
                    }
                val product =
                    ProductDAO.new {
                        userId = user.id
                        this.shopId = shop.id
                        categoryId = newCategory().id
                        name = "Concurrent Stock"
                        description = "stock test"
                        sku = "STOCK-${UUID.randomUUID()}"
                        price = BigDecimal("10.00")
                    }
                val warehouse =
                    com.piashcse.database.entities.WarehouseDAO.new {
                        this.shopId = shop.id
                        name = "Default"
                        code = "DEFAULT"
                    }
                InventoryDAO.new {
                    this.productId = product.id
                    this.shopId = shop.id
                    warehouseId = warehouse.id
                    stockQuantity = 5
                }
                productId = product.id
                shopId = shop.id
            }

            val results =
                runConcurrently(10) {
                    transaction {
                        ProductDAO.findById(productId.value)?.decrementStock(1)
                    }
                }

            assertEquals(5, results.count { it.isSuccess }, "Exactly the available stock should be sold")
            transaction {
                val stock = InventoryDAO.find { InventoryTable.productId eq productId }.first().stockQuantity
                assertEquals(0, stock, "Stock must never go negative")
            }
        }
    }

    @Test
    fun `concurrent order idempotency keys keep one row`() {
        withDatabase {
            lateinit var userId: EntityID<String>
            lateinit var shopId: EntityID<String>
            transaction {
                val user =
                    UserDAO.new {
                        email = "order-${UUID.randomUUID()}@example.com"
                        userType = UserType.CUSTOMER
                        password = "x"
                    }
                val shopCategory = ShopCategoryDAO.new { name = "Order Shops ${UUID.randomUUID()}" }
                val shop =
                    ShopDAO.new {
                        userId = user.id
                        categoryId = shopCategory.id
                        name = "Order Shop"
                        status = ShopStatus.APPROVED
                    }
                userId = user.id
                shopId = shop.id
            }

            runConcurrently(2) {
                transaction {
                    OrderDAO.new {
                        this.userId = userId
                        this.shopId = shopId
                        orderNumber = "ORD-${UUID.randomUUID()}"
                        idempotencyKey = "same-key"
                        subTotal = BigDecimal("10.00")
                        total = BigDecimal("10.00")
                    }
                }
            }

            transaction {
                val count = OrderDAO.find { OrderTable.idempotencyKey eq "same-key" }.count()
                assertEquals(1, count, "Per-user order idempotency must be unique")
                OrderDAO.find { OrderTable.idempotencyKey eq "same-key" }.forEach { it.delete() }
            }
        }
    }

    @Test
    fun `concurrent payment transaction ids keep one row`() {
        withDatabase {
            lateinit var orderId: EntityID<String>
            lateinit var userId: EntityID<String>
            transaction {
                val user =
                    UserDAO.new {
                        email = "pay-${UUID.randomUUID()}@example.com"
                        userType = UserType.CUSTOMER
                        password = "x"
                    }
                val shopCategory = ShopCategoryDAO.new { name = "Pay Shops ${UUID.randomUUID()}" }
                val shop =
                    ShopDAO.new {
                        userId = user.id
                        categoryId = shopCategory.id
                        name = "Pay Shop"
                        status = ShopStatus.APPROVED
                    }
                val order =
                    OrderDAO.new {
                        this.userId = user.id
                        this.shopId = shop.id
                        orderNumber = "ORD-${UUID.randomUUID()}"
                        subTotal = BigDecimal("10.00")
                        total = BigDecimal("10.00")
                    }
                orderId = order.id
                userId = user.id
            }

            runConcurrently(2) {
                transaction {
                    PaymentDAO.new {
                        this.orderId = orderId
                        this.userId = userId
                        amount = BigDecimal("10.00")
                        paymentMethod = PaymentMethod.COD
                        transactionId = "same-tx"
                    }
                }
            }

            transaction {
                val count = PaymentDAO.find { PaymentTable.transactionId eq "same-tx" }.count()
                assertEquals(1, count, "Payment transaction_id must be unique")
                PaymentDAO.find { PaymentTable.transactionId eq "same-tx" }.forEach { it.delete() }
            }
        }
    }

    @Test
    fun `concurrent payout request keys keep one request`() {
        withDatabase {
            lateinit var sellerId: EntityID<String>
            transaction {
                val user =
                    UserDAO.new {
                        email = "payout-${UUID.randomUUID()}@example.com"
                        userType = UserType.SELLER
                        password = "x"
                    }
                val seller = SellerDAO.new { userId = user.id }
                sellerId = seller.id
            }

            runConcurrently(2) {
                transaction {
                    PayoutRequestDAO.new {
                        this.sellerId = sellerId
                        idempotencyKey = "same-payout-key"
                        amount = BigDecimal("10.00")
                    }
                }
            }

            transaction {
                val count = PayoutRequestDAO.find { PayoutRequestTable.idempotencyKey eq "same-payout-key" }.count()
                assertEquals(1, count, "Per-seller payout idempotency must be unique")
                PayoutRequestDAO.find { PayoutRequestTable.idempotencyKey eq "same-payout-key" }.forEach { it.delete() }
            }
        }
    }

    @Test
    fun `concurrent coupon usage rows keep one row`() {
        withDatabase {
            lateinit var couponId: EntityID<String>
            lateinit var userId: EntityID<String>
            lateinit var orderId: EntityID<String>
            transaction {
                val user =
                    UserDAO.new {
                        email = "coupon-${UUID.randomUUID()}@example.com"
                        userType = UserType.CUSTOMER
                        password = "x"
                    }
                val shopCategory = ShopCategoryDAO.new { name = "Coupon Shops ${UUID.randomUUID()}" }
                val shop =
                    ShopDAO.new {
                        userId = user.id
                        categoryId = shopCategory.id
                        name = "Coupon Shop"
                        status = ShopStatus.APPROVED
                    }
                val order =
                    OrderDAO.new {
                        this.userId = user.id
                        this.shopId = shop.id
                        orderNumber = "ORD-${UUID.randomUUID()}"
                        subTotal = BigDecimal("10.00")
                        total = BigDecimal("10.00")
                    }
                val coupon =
                    CouponDAO.new {
                        code = "CONC-${UUID.randomUUID()}"
                        discountType = CouponDiscountType.FIXED
                        discountValue = BigDecimal("1.00")
                        startDate = com.piashcse.database.entities.base.currentUtc().minusDays(1)
                        endDate = com.piashcse.database.entities.base.currentUtc().plusDays(1)
                    }
                couponId = coupon.id
                userId = user.id
                orderId = order.id
            }

            runConcurrently(2) {
                transaction {
                    CouponUsageDAO.new {
                        this.couponId = couponId
                        this.userId = userId
                        this.orderId = orderId
                    }
                }
            }

            transaction {
                val count =
                    CouponUsageDAO.find {
                        (CouponUsageTable.couponId eq couponId) and
                            (CouponUsageTable.userId eq userId) and
                            (CouponUsageTable.orderId eq orderId)
                    }.count()
                assertEquals(1, count, "Coupon usage must be unique per coupon/user/order")
                CouponUsageDAO.find { CouponUsageTable.couponId eq couponId }.forEach { it.delete() }
            }
        }
    }
}
