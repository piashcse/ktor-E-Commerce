package com.piashcse.database

import com.piashcse.constants.BillingInterval
import com.piashcse.constants.InvoiceStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.SubscriptionStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.DigitalEntitlementDAO
import com.piashcse.database.entities.DigitalEntitlementTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.SubscriptionInvoiceDAO
import com.piashcse.database.entities.SubscriptionInvoiceTable
import com.piashcse.database.entities.SubscriptionTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.feature.subscription.SubscribeRequest
import com.piashcse.feature.subscription.SubscriptionPlanRequest
import com.piashcse.feature.subscription.SubscriptionService
import com.piashcse.utils.extension.entityID
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assume
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Subscriptions, invoices and digital entitlements against PostgreSQL. */
class SubscriptionIntegrationTest {
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
    fun `subscriptions renew and grant digital access`() {
        val external = envDb()
        if (external != null) {
            runSubscriptionFlow(external.first, external.second, external.third)
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
            runSubscriptionFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runSubscriptionFlow(
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
        lateinit var productId: String
        lateinit var orderId: String

        transaction {
            val seller =
                UserDAO.new {
                    email = "subscription-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            val customer =
                UserDAO.new {
                    email = "subscription-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            customerId = customer.id.value
            val shopCategory = ShopCategoryDAO.new { name = "Sub $suffix" }
            val shop =
                ShopDAO.new {
                    userId = seller.id
                    categoryId = shopCategory.id
                    name = "Sub Shop $suffix"
                    status = ShopStatus.APPROVED
                }
            val category = ProductCategoryDAO.new { name = "Sub Cat $suffix" }
            val product =
                ProductDAO.new {
                    userId = seller.id
                    shopId = shop.id
                    categoryId = category.id
                    name = "Digital Pack $suffix"
                    description = "Downloadable pack"
                    sku = "DIGITAL-$suffix"
                    price = BigDecimal("9.99")
                    status = ProductStatus.ACTIVE
                    isDigital = true
                    digitalAssetUrl = "https://downloads.example/$suffix.zip"
                    digitalFileName = "pack-$suffix.zip"
                }
            productId = product.id.value
            val order =
                OrderDAO.new {
                    userId = customer.id
                    shopId = shop.id
                    orderNumber = "SUB-$suffix"
                    subTotal = BigDecimal("9.99")
                    total = BigDecimal("9.99")
                    status = OrderStatus.PAID
                    paymentStatus = PaymentStatus.COMPLETED
                }
            orderId = order.id.value
            OrderItemDAO.new {
                this.orderId = order.id
                this.productId = product.id
                shopId = shop.id
                quantity = 1
                price = BigDecimal("9.99")
                total = BigDecimal("9.99")
                productName = product.name
                isDigital = true
            }
        }

        val plans = SubscriptionService()
        val plan =
            kotlinx.coroutines.runBlocking {
                plans.createPlan(
                    SubscriptionPlanRequest(
                        name = "Pro Monthly",
                        description = "Digital pack access",
                        productId = productId,
                        interval = BillingInterval.MONTH,
                        price = BigDecimal("9.99"),
                        trialDays = 0,
                    ),
                )
            }
        val subscription =
            kotlinx.coroutines.runBlocking {
                plans.subscribe(customerId, SubscribeRequest(plan.id, paymentReference = "pay_initial"))
            }
        assertEquals(SubscriptionStatus.ACTIVE, subscription.status)
        assertEquals("Pro Monthly", subscription.planName)

        val invoices = kotlinx.coroutines.runBlocking { plans.invoicesForUser(customerId, subscription.id) }
        assertEquals(1, invoices.size)
        assertEquals(InvoiceStatus.PAID, invoices.single().status)

        val deliveries = DigitalDeliveryService()
        val initialLibrary = kotlinx.coroutines.runBlocking { deliveries.library(customerId) }
        assertEquals(1, initialLibrary.size)
        assertEquals(productId, initialLibrary.single().productId)
        val token = initialLibrary.single().downloadToken
        val download = kotlinx.coroutines.runBlocking { deliveries.download(token, customerId) }
        assertEquals(49, download.remainingDownloads)
        assertEquals("pack-$suffix.zip", download.fileName)
        assertTrue(download.downloadUrl.contains("/api/v1/library/asset"))
        val signed = java.net.URI(download.downloadUrl)
        val params =
            signed.query.split("&").associate {
                val (key, value) = it.split("=", limit = 2)
                key to java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8)
            }
        assertEquals(
            "https://downloads.example/$suffix.zip",
            kotlinx.coroutines.runBlocking {
                deliveries.asset(params.getValue("token"), params.getValue("expires").toLong(), params.getValue("signature"))
            },
        )

        // Payment for a one-off digital order grants a second entitlement.
        transaction {
            val order = OrderDAO.findById(orderId)!!
            deliveries.grantForOrderTx(order)
        }
        val afterOrder = kotlinx.coroutines.runBlocking { deliveries.library(customerId) }
        assertEquals(2, afterOrder.size)
        transaction { deliveries.revokeOrderTx(orderId) }
        assertEquals(
            1,
            kotlinx.coroutines.runBlocking { deliveries.library(customerId) }.size,
            "Order refund/cancel must revoke order-based digital access",
        )
        assertEquals(
            1,
            transaction {
                DigitalEntitlementDAO.find {
                    DigitalEntitlementTable.userId eq customerId.entityID(com.piashcse.database.entities.UserTable)
                }.count().toInt()
            },
        )

        // Renew after the period ends.
        val renewal =
            kotlinx.coroutines.runBlocking {
                plans.runBilling(currentUtc().plusDays(40))
            }
        assertTrue(renewal.invoicesCreated >= 1)
        val invoicesAfter = kotlinx.coroutines.runBlocking { plans.invoicesForUser(customerId, subscription.id) }
        assertEquals(2, invoicesAfter.size)
        assertEquals(1, invoicesAfter.count { it.status.name == "PENDING" })
        assertTrue(
            transaction {
                SubscriptionInvoiceDAO.find {
                    SubscriptionInvoiceTable.subscriptionId eq subscription.id.entityID(SubscriptionTable)
                }.count() == 2L
            },
        )
        val activated = kotlinx.coroutines.runBlocking { plans.activate(subscription.id, "pay_renewal") }
        assertEquals(SubscriptionStatus.ACTIVE, activated.status)

        // Cancel at period end; the next run cancels instead of invoicing.
        kotlinx.coroutines.runBlocking { plans.cancel(customerId, subscription.id) }
        val canceled = kotlinx.coroutines.runBlocking { plans.runBilling(currentUtc().plusDays(80)) }
        assertEquals(1, canceled.canceled)
        assertEquals(
            SubscriptionStatus.CANCELED,
            kotlinx.coroutines.runBlocking { plans.mySubscriptions(customerId) }.single().status,
        )
    }
}
