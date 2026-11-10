package com.piashcse.database

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ScaStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PaymentWebhookEventDAO
import com.piashcse.database.entities.PaymentWebhookEventTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.feature.payment.GatewayChargeResult
import com.piashcse.feature.payment.GatewayEventType
import com.piashcse.feature.payment.GatewayRefundResult
import com.piashcse.feature.payment.GatewayWebhookEvent
import com.piashcse.feature.payment.PaymentGateway
import com.piashcse.feature.payment.PaymentRepositoryImpl
import com.piashcse.utils.validator.ValidationException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Signed provider webhooks against a real PostgreSQL database. */
class PaymentWebhookIntegrationTest {
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
    fun `signed webhook completes a payment exactly once`() {
        val external = envDb()
        if (external != null) {
            runWebhookFlow(external.first, external.second, external.third)
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
            runWebhookFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runWebhookFlow(
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
        lateinit var orderId: String
        lateinit var paymentId: String
        val reference = "pi_$suffix"

        transaction {
            val customer =
                UserDAO.new {
                    email = "webhook-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val order =
                OrderDAO.new {
                    userId = customer.id
                    orderNumber = "WEBHOOK-$suffix"
                    subTotal = BigDecimal("10.00")
                    total = BigDecimal("10.00")
                }
            orderId = order.id.value
            val payment =
                PaymentDAO.new {
                    this.orderId = order.id
                    this.userId = customer.id
                    amount = BigDecimal("10.00")
                    currency = "USD"
                    paymentMethod = PaymentMethod.CREDIT_CARD
                    status = PaymentStatus.PENDING
                    provider = PaymentProvider.STRIPE
                    providerReference = reference
                    providerStatus = "requires_action"
                    scaStatus = ScaStatus.REQUIRED
                    transactionId = reference
                }
            paymentId = payment.id.value
        }

        val gateway =
            object : PaymentGateway {
                override val name: String = "fake-stripe"
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
                ): GatewayChargeResult = GatewayChargeResult(true)

                override suspend fun refund(
                    providerReference: String,
                    amount: BigDecimal,
                    currency: String,
                    idempotencyKey: String,
                ): GatewayRefundResult = GatewayRefundResult(true, "re_1", "succeeded")

                override fun verifyWebhook(
                    payload: String,
                    signature: String?,
                ): Boolean = signature == "sig"

                override fun parseWebhook(payload: String): GatewayWebhookEvent =
                    GatewayWebhookEvent(
                        eventId = "evt_$suffix",
                        provider = PaymentProvider.STRIPE,
                        type = GatewayEventType.PAYMENT_SUCCEEDED,
                        providerReference = reference,
                        amount = BigDecimal("10.00"),
                        currency = "USD",
                        scaStatus = ScaStatus.AUTHENTICATED,
                        providerStatus = "succeeded",
                    )
            }

        val repo = PaymentRepositoryImpl(gateway)
        val first = kotlinx.coroutines.runBlocking { repo.handleWebhook("stripe", "{}", "sig") }
        assertTrue(first.accepted)
        assertTrue(!first.duplicate)

        transaction {
            val payment = PaymentDAO.findById(paymentId)!!
            assertEquals(PaymentStatus.COMPLETED, payment.status)
            assertEquals(ScaStatus.AUTHENTICATED, payment.scaStatus)
            val order = OrderDAO.findById(orderId)!!
            assertEquals(PaymentStatus.COMPLETED, order.paymentStatus)
            assertEquals(OrderStatus.PAID, order.status)
            assertEquals(
                1,
                PaymentWebhookEventDAO.find { PaymentWebhookEventTable.eventId eq "evt_$suffix" }.count().toInt(),
            )
        }

        val duplicate = kotlinx.coroutines.runBlocking { repo.handleWebhook("stripe", "{}", "sig") }
        assertTrue(duplicate.accepted)
        assertTrue(duplicate.duplicate)

        assertFailsWith<ValidationException> {
            kotlinx.coroutines.runBlocking { repo.handleWebhook("stripe", "{}", "bad") }
        }
        assertFailsWith<com.piashcse.utils.validator.NotFoundException> {
            kotlinx.coroutines.runBlocking { repo.handleWebhook("unknown", "{}", "sig") }
        }
    }
}
