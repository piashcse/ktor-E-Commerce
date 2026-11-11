package com.piashcse.database

import com.piashcse.constants.AnalyticsEventType
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.analytics.AnalyticsEventRequest
import com.piashcse.feature.analytics.AnalyticsService
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assume
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Funnel, attribution and product conversion against PostgreSQL. */
class AnalyticsIntegrationTest {
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
    fun `funnel attribution and product conversion are reported`() {
        val external = envDb()
        if (external != null) {
            runAnalyticsFlow(external.first, external.second, external.third)
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
            runAnalyticsFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runAnalyticsFlow(
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
        lateinit var productId: String
        val today = currentUtc().toLocalDate().toString()

        transaction {
            val seller =
                UserDAO.new {
                    email = "analytics-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            val customer =
                UserDAO.new {
                    email = "analytics-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val shopCategory = ShopCategoryDAO.new { name = "Analytics $suffix" }
            val shop =
                ShopDAO.new {
                    userId = seller.id
                    categoryId = shopCategory.id
                    name = "Analytics Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            val category = ProductCategoryDAO.new { name = "Analytics Cat $suffix" }
            val product =
                ProductDAO.new {
                    userId = seller.id
                    shopId = shop.id
                    categoryId = category.id
                    name = "Analytics Product $suffix"
                    description = "Tracked product"
                    sku = "AN-$suffix"
                    price = BigDecimal("50.00")
                    status = ProductStatus.ACTIVE
                }
            productId = product.id.value
            val order =
                OrderDAO.new {
                    userId = customer.id
                    shopId = shop.id
                    orderNumber = "AN-$suffix"
                    subTotal = BigDecimal("100.00")
                    total = BigDecimal("100.00")
                    status = OrderStatus.PAID
                    paymentStatus = PaymentStatus.COMPLETED
                    attributionSessionId = "s-1"
                    attributionSource = "google"
                    attributionMedium = "cpc"
                    attributionCampaign = "spring"
                }
            OrderItemDAO.new {
                orderId = order.id
                this.productId = product.id
                shopId = shop.id
                quantity = 2
                price = BigDecimal("50.00")
                total = BigDecimal("100.00")
                productName = product.name
            }
        }

        val analytics = AnalyticsService()
        kotlinx.coroutines.runBlocking {
            analytics.track(
                userId = null,
                events =
                    listOf(
                        AnalyticsEventRequest(AnalyticsEventType.PAGE_VIEW, "s-1", path = "/"),
                        AnalyticsEventRequest(AnalyticsEventType.PRODUCT_VIEW, "s-1", productId = productId, path = "/products/$productId"),
                        AnalyticsEventRequest(AnalyticsEventType.ADD_TO_CART, "s-1", productId = productId),
                        AnalyticsEventRequest(AnalyticsEventType.CHECKOUT_START, "s-1"),
                        AnalyticsEventRequest(AnalyticsEventType.PAGE_VIEW, "s-2", path = "/"),
                        AnalyticsEventRequest(
                            AnalyticsEventType.PRODUCT_VIEW,
                            "s-2",
                            productId = productId,
                            utmSource = "facebook",
                            utmMedium = "social",
                        ),
                    ),
            )
        }

        val funnel = kotlinx.coroutines.runBlocking { analytics.funnel(today, today) }
        assertEquals(listOf("page_view", "product_view", "add_to_cart", "checkout_start", "purchase"), funnel.stages.map { it.name })
        assertEquals(2, funnel.stages[0].sessions)
        assertEquals(2, funnel.stages[1].sessions)
        assertEquals(1, funnel.stages[2].sessions)
        assertEquals(1, funnel.stages[3].sessions)
        assertTrue(funnel.stages[4].sessions >= 1)

        val attribution = kotlinx.coroutines.runBlocking { analytics.attribution(today, today) }
        val google = attribution.rows.first { it.source == "google" }
        assertEquals(1, google.orders)
        assertEquals("100.00", google.revenueUsd)
        assertEquals("spring", google.campaign)

        val products = kotlinx.coroutines.runBlocking { analytics.productFunnel(today, today, 10) }
        val product = products.products.first { it.productId == productId }
        assertEquals(2, product.views)
        assertEquals(1, product.addToCart)
        assertEquals(2, product.purchases)
        assertEquals("100.00", product.revenue)
    }
}
