package com.piashcse.database

import com.piashcse.constants.BillingInterval
import com.piashcse.constants.InvoiceStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
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
import com.piashcse.feature.payment.GatewayChargeResult
import com.piashcse.feature.payment.GatewayRefundResult
import com.piashcse.feature.payment.GatewayWebhookEvent
import com.piashcse.feature.payment.PaymentGateway
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.feature.subscription.SubscribeRequest
import com.piashcse.feature.subscription.SubscriptionBillingService
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

    private class FakeRecurringGateway : PaymentGateway {
        val failing = mutableSetOf<String>()
        override val name: String = "FAKE"
        override val provider: PaymentProvider = PaymentProvider.STRIPE

        override fun supports(paymentMethod: PaymentMethod): Boolean = true

        override suspend fun charge(
            orderId: String,
            amount: BigDecimal,
            currency: String,
            paymentMethod: PaymentMethod,
            transactionId: String?,
            returnUrl: String?,
            idempotencyKey: String?,
            require3ds: Boolean,
        ): GatewayChargeResult = GatewayChargeResult(false, message = "not used")

        override suspend fun refund(
            providerReference: String,
            amount: BigDecimal,
            currency: String,
            idempotencyKey: String,
        ): GatewayRefundResult = GatewayRefundResult(false, message = "not used")

        override fun verifyWebhook(
            payload: String,
            signature: String?,
        ): Boolean = false

        override fun parseWebhook(payload: String): GatewayWebhookEvent =
            GatewayWebhookEvent("fake", PaymentProvider.STRIPE, com.piashcse.feature.payment.GatewayEventType.UNKNOWN)

        override suspend fun chargeSubscription(
            subscriptionId: String,
            invoiceId: String,
            amount: BigDecimal,
            currency: String,
            providerCustomerReference: String?,
            paymentMethodReference: String,
            idempotencyKey: String,
        ): GatewayChargeResult =
            if (subscriptionId in failing) {
                GatewayChargeResult(false, message = "card declined")
            } else {
                GatewayChargeResult(
                    success = true,
                    captured = true,
                    gatewayTransactionId = "pi_$invoiceId",
                    providerStatus = "succeeded",
                )
            }
    }

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
            kotlinx.coroutines.runBlocking { plans.mySubscriptions(customerId) }
                .first { it.id == subscription.id }.status,
        )

        // A stored payment method is charged off-session on renewal.
        val recurringPlan =
            kotlinx.coroutines.runBlocking {
                plans.createPlan(
                    SubscriptionPlanRequest(
                        name = "Recurring Monthly",
                        description = "Stored-card renewal",
                        productId = productId,
                        interval = BillingInterval.MONTH,
                        price = BigDecimal("4.99"),
                    ),
                )
            }
        val recurringDeliveries = DigitalDeliveryService()
        val gateway = FakeRecurringGateway()
        val recurring =
            SubscriptionService(
                recurringDeliveries,
                SubscriptionBillingService(recurringDeliveries, gateway, graceDays = 3, maxAttempts = 2),
            )
        val recurringSubscription =
            kotlinx.coroutines.runBlocking {
                recurring.subscribe(
                    customerId,
                    SubscribeRequest(
                        recurringPlan.id,
                        paymentReference = "pay_recurring_initial",
                        paymentMethodReference = "pm_test",
                        providerCustomerReference = "cus_test",
                    ),
                )
            }
        val renewed = kotlinx.coroutines.runBlocking { recurring.runBilling(currentUtc().plusDays(100)) }
        assertTrue(renewed.paid >= 1)
        assertEquals(
            SubscriptionStatus.ACTIVE,
            kotlinx.coroutines.runBlocking { recurring.mySubscriptions(customerId) }
                .first { it.id == recurringSubscription.id }.status,
        )
        assertEquals(
            2,
            kotlinx.coroutines.runBlocking { recurring.invoicesForUser(customerId, recurringSubscription.id) }.size,
        )

        // Failed renewals enter dunning and expire after the grace period.
        val dunningPlan =
            kotlinx.coroutines.runBlocking {
                plans.createPlan(
                    SubscriptionPlanRequest(
                        name = "Dunning Monthly",
                        description = "Declined-card renewal",
                        productId = productId,
                        interval = BillingInterval.MONTH,
                        price = BigDecimal("2.99"),
                    ),
                )
            }
        val dunningSubscription =
            kotlinx.coroutines.runBlocking {
                recurring.subscribe(
                    customerId,
                    SubscribeRequest(
                        dunningPlan.id,
                        paymentReference = "pay_dunning_initial",
                        paymentMethodReference = "pm_declined",
                    ),
                )
            }
        gateway.failing += dunningSubscription.id
        val failedRun = kotlinx.coroutines.runBlocking { recurring.runBilling(currentUtc().plusDays(130)) }
        assertTrue(failedRun.failed >= 1)
        assertEquals(
            SubscriptionStatus.PAST_DUE,
            kotlinx.coroutines.runBlocking { recurring.mySubscriptions(customerId) }
                .first { it.id == dunningSubscription.id }.status,
        )
        val retryRun = kotlinx.coroutines.runBlocking { recurring.runBilling(currentUtc().plusDays(132)) }
        assertTrue(retryRun.expired >= 1)
        assertEquals(
            2,
            transaction {
                SubscriptionInvoiceDAO.find {
                    SubscriptionInvoiceTable.subscriptionId eq dunningSubscription.id.entityID(SubscriptionTable)
                }.maxOf { it.attemptCount }
            },
        )
        assertEquals(
            SubscriptionStatus.EXPIRED,
            kotlinx.coroutines.runBlocking { recurring.mySubscriptions(customerId) }
                .first { it.id == dunningSubscription.id }.status,
        )

        assertEquals(
            2,
            transaction {
                SubscriptionInvoiceDAO.find {
                    SubscriptionInvoiceTable.subscriptionId eq dunningSubscription.id.entityID(SubscriptionTable)
                }.maxOf { it.attemptCount }
            },
        )
    }
}
