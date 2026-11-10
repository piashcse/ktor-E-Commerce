package com.piashcse.database

import com.piashcse.constants.AvsResult
import com.piashcse.constants.BlocklistType
import com.piashcse.constants.ChargebackStatus
import com.piashcse.constants.CvvResult
import com.piashcse.constants.FraudMetric
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RiskAction
import com.piashcse.constants.RiskDecision
import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChargebackDAO
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PaymentRiskDAO
import com.piashcse.database.entities.PaymentRiskTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.feature.fraud.BlocklistRequest
import com.piashcse.feature.fraud.ChargebackRequest
import com.piashcse.feature.fraud.ChargebackService
import com.piashcse.feature.fraud.ChargebackUpdateRequest
import com.piashcse.feature.fraud.FraudContext
import com.piashcse.feature.fraud.FraudRuleRequest
import com.piashcse.feature.fraud.FraudService
import com.piashcse.feature.fraud.RiskResultRequest
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Fraud rules, blocklists, AVS/CVV storage and chargebacks against PostgreSQL. */
class FraudIntegrationTest {
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
    fun `risk decisions and chargebacks persist`() {
        val external = envDb()
        if (external != null) {
            runFraudFlow(external.first, external.second, external.third)
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
            runFraudFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runFraudFlow(
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
        lateinit var userId: String
        lateinit var email: String
        lateinit var orderId: String
        lateinit var paymentId: String

        transaction {
            val customer =
                UserDAO.new {
                    this.email = "fraud-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            userId = customer.id.value
            email = customer.email
            val order =
                OrderDAO.new {
                    this.userId = customer.id
                    orderNumber = "FRAUD-$suffix"
                    subTotal = BigDecimal("50.00")
                    total = BigDecimal("50.00")
                }
            orderId = order.id.value
            val payment =
                PaymentDAO.new {
                    this.orderId = order.id
                    this.userId = customer.id
                    amount = BigDecimal("50.00")
                    paymentMethod = PaymentMethod.COD
                    status = PaymentStatus.COMPLETED
                }
            paymentId = payment.id.value
        }

        val fraud = FraudService()
        val context = FraudContext(userId, email, "203.0.113.10", BigDecimal("50.00"))
        assertEquals(RiskDecision.ALLOW, transaction { fraud.evaluate(context) }.decision)

        val block =
            kotlinx.coroutines.runBlocking {
                fraud.createBlocklistEntry(BlocklistRequest(BlocklistType.USER, userId, "confirmed fraud"))
            }
        val blocked = transaction { fraud.evaluate(context) }
        assertEquals(RiskDecision.BLOCK, blocked.decision)
        assertTrue(blocked.blocked)
        assertTrue(kotlinx.coroutines.runBlocking { fraud.deleteBlocklistEntry(block.id) })

        val rule =
            kotlinx.coroutines.runBlocking {
                fraud.createRule(
                    FraudRuleRequest(
                        name = "More than one order",
                        metric = FraudMetric.ORDER_COUNT,
                        windowMinutes = 60,
                        threshold = BigDecimal.ONE,
                        action = RiskAction.REVIEW,
                        priority = 10,
                    ),
                )
            }
        assertEquals(RiskDecision.REVIEW, transaction { fraud.evaluate(context) }.decision)
        assertTrue(kotlinx.coroutines.runBlocking { fraud.deleteRule(rule.id) })

        val amountRule =
            kotlinx.coroutines.runBlocking {
                fraud.createRule(
                    FraudRuleRequest(
                        name = "Large basket",
                        metric = FraudMetric.ORDER_AMOUNT,
                        windowMinutes = 60,
                        threshold = BigDecimal("25.00"),
                        action = RiskAction.BLOCK,
                        priority = 20,
                    ),
                )
            }
        assertTrue(transaction { fraud.evaluate(context) }.blocked)
        assertTrue(kotlinx.coroutines.runBlocking { fraud.deleteRule(amountRule.id) })

        // Persist a risk decision + AVS/CVV on the payment.
        transaction {
            val payment = PaymentDAO.findById(paymentId)!!
            fraud.recordRisk(
                payment,
                com.piashcse.feature.fraud.RiskAssessment(
                    decision = RiskDecision.REVIEW,
                    score = 50,
                    reasons = listOf("manual review"),
                ),
            )
        }
        kotlinx.coroutines.runBlocking {
            fraud.recordRiskResult(paymentId, RiskResultRequest(AvsResult.MATCH, CvvResult.MISMATCH))
        }
        transaction {
            val payment = PaymentDAO.findById(paymentId)!!
            assertEquals(RiskDecision.REVIEW, payment.riskDecision)
            assertEquals(AvsResult.MATCH, payment.avsStatus)
            assertEquals(CvvResult.MISMATCH, payment.cvvStatus)
            assertEquals(
                2,
                PaymentRiskDAO.find { PaymentRiskTable.paymentId eq payment.id }.count().toInt(),
            )
        }

        // Chargeback lifecycle.
        val chargebacks = ChargebackService()
        val chargeback =
            kotlinx.coroutines.runBlocking {
                chargebacks.create(
                    ChargebackRequest(
                        paymentId = paymentId,
                        providerDisputeId = "dp-$suffix",
                        amount = BigDecimal("25.00"),
                        reason = "Fraudulent card use",
                    ),
                )
            }
        assertEquals(ChargebackStatus.OPEN, chargeback.status)
        assertEquals("25.00", chargeback.amount)
        val reviewing =
            kotlinx.coroutines.runBlocking {
                chargebacks.update(chargeback.id, ChargebackUpdateRequest(ChargebackStatus.UNDER_REVIEW))
            }
        assertEquals(ChargebackStatus.UNDER_REVIEW, reviewing.status)
        val won =
            kotlinx.coroutines.runBlocking {
                chargebacks.update(
                    chargeback.id,
                    ChargebackUpdateRequest(ChargebackStatus.WON, outcome = "Evidence accepted"),
                )
            }
        assertEquals(ChargebackStatus.WON, won.status)
        assertNotNull(won.resolvedAt)
        transaction {
            val row = ChargebackDAO.findById(chargeback.id)!!
            assertEquals(orderId, row.orderId.value)
            assertNotNull(row.resolvedAt)
        }
    }
}
