package com.piashcse.database

import com.piashcse.constants.DisputeStatus
import com.piashcse.constants.DisputeType
import com.piashcse.constants.KycStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.ReviewStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.feature.seller.CommissionRuleRequest
import com.piashcse.feature.seller.CommissionService
import com.piashcse.feature.seller.DisputeRequest
import com.piashcse.feature.seller.DisputeUpdateRequest
import com.piashcse.feature.seller.SellerReviewRequest
import com.piashcse.feature.seller.SellerToolsService
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assume
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Seller reviews, disputes and commission rules against PostgreSQL. */
class SellerToolsIntegrationTest {
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

    @Test
    fun `reviews disputes and commission rules persist`() {
        val external = envDb()
        if (external != null) {
            runSellerFlow(external.first, external.second, external.third)
            return
        }
        if (System.getenv("CI") != null) {
            assertTrue(dockerAvailable(), "DB_* environment or Docker is required in CI")
        } else {
            Assume.assumeTrue("Docker daemon or DB_* environment required", dockerAvailable())
        }
        val pg = PostgreSQLContainer("postgres:16-alpine")
        pg.start()
        try {
            runSellerFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runSellerFlow(
        jdbcUrl: String,
        user: String,
        dbPassword: String,
    ) {
        Flyway.configure()
            .dataSource(jdbcUrl, user, dbPassword)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        Database.connect(jdbcUrl, user = user, password = dbPassword)

        val suffix = UUID.randomUUID().toString().take(8)
        lateinit var customerId: String
        lateinit var sellerUserId: String
        lateinit var orderId: String
        lateinit var sellerId: String
        lateinit var shopId: String
        lateinit var categoryId: String

        transaction {
            val customer =
                UserDAO.new {
                    email = "tools-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val sellerUser =
                UserDAO.new {
                    email = "tools-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            customerId = customer.id.value
            sellerUserId = sellerUser.id.value
            val shopCategory = ShopCategoryDAO.new { name = "Tools $suffix" }
            val shop =
                ShopDAO.new {
                    userId = sellerUser.id
                    this.categoryId = shopCategory.id
                    name = "Tools Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            shopId = shop.id.value
            val seller =
                SellerDAO.new {
                    userId = sellerUser.id
                    this.shopId = shop.id
                    status = ShopStatus.APPROVED
                    kycStatus = KycStatus.NOT_SUBMITTED
                    commissionRate = BigDecimal("10.00")
                }
            sellerId = seller.id.value
            val category = ProductCategoryDAO.new { name = "Tools Cat $suffix" }
            categoryId = category.id.value
            val product =
                ProductDAO.new {
                    userId = sellerUser.id
                    this.shopId = shop.id
                    this.categoryId = category.id
                    name = "Tools Product $suffix"
                    description = "Seller tools test product"
                    sku = "TOOLS-$suffix"
                    price = BigDecimal("100.00")
                    status = ProductStatus.ACTIVE
                }
            val order =
                OrderDAO.new {
                    userId = customer.id
                    this.shopId = shop.id
                    orderNumber = "TOOLS-$suffix"
                    subTotal = BigDecimal("100.00")
                    total = BigDecimal("100.00")
                    status = OrderStatus.DELIVERED
                    paymentStatus = PaymentStatus.COMPLETED
                }
            orderId = order.id.value
            OrderItemDAO.new {
                this.orderId = order.id
                this.productId = product.id
                this.shopId = shop.id
                quantity = 1
                price = BigDecimal("100.00")
                total = BigDecimal("100.00")
                productName = product.name
            }
        }

        val tools = SellerToolsService()
        val review = kotlinx.coroutines.runBlocking { tools.createReview(customerId, SellerReviewRequest(orderId, 5, "Great seller")) }
        assertEquals(5, review.rating)
        assertEquals("5.00", transaction { ShopDAO.findById(shopId)!!.rating.toPlainString() })
        assertEquals(1, transaction { ShopDAO.findById(shopId)!!.totalReviews })
        assertEquals(
            1,
            kotlinx.coroutines.runBlocking { tools.reviewsForSeller(sellerId, 10, 0) }.data.size,
        )
        kotlinx.coroutines.runBlocking { tools.updateReviewStatus(review.id, ReviewStatus.HIDDEN) }
        assertEquals("0.00", transaction { ShopDAO.findById(shopId)!!.rating.toPlainString() })

        val dispute =
            kotlinx.coroutines.runBlocking {
                tools.createDispute(
                    customerId,
                    DisputeRequest(
                        orderId = orderId,
                        type = DisputeType.ORDER_ISSUE,
                        subject = "Missing item",
                        description = "One item was missing",
                    ),
                )
            }
        assertEquals(DisputeStatus.OPEN, dispute.status)
        val reviewing =
            kotlinx.coroutines.runBlocking {
                tools.updateDispute(dispute.id, sellerUserId, DisputeUpdateRequest(DisputeStatus.UNDER_REVIEW))
            }
        assertEquals(DisputeStatus.UNDER_REVIEW, reviewing.status)
        val resolved =
            kotlinx.coroutines.runBlocking {
                tools.updateDispute(
                    dispute.id,
                    sellerUserId,
                    DisputeUpdateRequest(DisputeStatus.RESOLVED, "Replacement sent"),
                )
            }
        assertEquals(DisputeStatus.RESOLVED, resolved.status)
        assertNotNull(resolved.resolvedAt)

        // Commission specificity: seller > shop > category > default.
        val commission = CommissionService()
        val sellRule =
            kotlinx.coroutines.runBlocking {
                commission.create(CommissionRuleRequest("Seller rate", sellerId = sellerId, rate = BigDecimal("3.00"), priority = 10))
            }
        val shopRule =
            kotlinx.coroutines.runBlocking {
                commission.create(CommissionRuleRequest("Shop rate", shopId = shopId, rate = BigDecimal("7.00")))
            }
        val categoryRule =
            kotlinx.coroutines.runBlocking {
                commission.create(CommissionRuleRequest("Category rate", categoryId = categoryId, rate = BigDecimal("5.00")))
            }
        val defaultRule =
            kotlinx.coroutines.runBlocking {
                commission.create(CommissionRuleRequest("Default rate", rate = BigDecimal("10.00")))
            }
        assertEquals("3.00", kotlinx.coroutines.runBlocking { commission.effectiveRate(sellerUserId, categoryId) }.rate)
        kotlinx.coroutines.runBlocking { commission.delete(sellRule.id) }
        assertEquals("7.00", kotlinx.coroutines.runBlocking { commission.effectiveRate(sellerUserId, categoryId) }.rate)
        kotlinx.coroutines.runBlocking { commission.delete(shopRule.id) }
        assertEquals("5.00", kotlinx.coroutines.runBlocking { commission.effectiveRate(sellerUserId, categoryId) }.rate)
        kotlinx.coroutines.runBlocking { commission.delete(categoryRule.id) }
        assertEquals("10.00", kotlinx.coroutines.runBlocking { commission.effectiveRate(sellerUserId, categoryId) }.rate)

        val orderCommission =
            transaction {
                val order = OrderDAO.findById(orderId)!!
                val seller = SellerDAO.findById(sellerId)!!
                commission.commissionForOrderTx(order, seller)
            }
        assertEquals(0, orderCommission.compareTo(BigDecimal("10.00")))
        kotlinx.coroutines.runBlocking { commission.delete(defaultRule.id) }
    }
}
