package com.piashcse.database

import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.feature.loyalty.AdminCreditRequest
import com.piashcse.feature.loyalty.GiftCardIssueRequest
import com.piashcse.feature.loyalty.LoyaltyService
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

/** Loyalty points, referrals, store credit and gift cards against PostgreSQL. */
class LoyaltyIntegrationTest {
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
    fun `points referrals credit and gift cards persist`() {
        val external = envDb()
        if (external != null) {
            runLoyaltyFlow(external.first, external.second, external.third)
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
            runLoyaltyFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runLoyaltyFlow(
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
        lateinit var referrerId: String
        lateinit var refereeId: String
        lateinit var orderId: String

        transaction {
            val referrer =
                UserDAO.new {
                    email = "loyalty-referrer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val referee =
                UserDAO.new {
                    email = "loyalty-referee-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            referrerId = referrer.id.value
            refereeId = referee.id.value
            val order =
                OrderDAO.new {
                    userId = referee.id
                    orderNumber = "LOYALTY-$suffix"
                    subTotal = BigDecimal("100.00")
                    total = BigDecimal("100.00")
                    paymentStatus = PaymentStatus.COMPLETED
                }
            orderId = order.id.value
        }

        val loyalty = LoyaltyService()
        val referrerWallet = kotlinx.coroutines.runBlocking { loyalty.wallet(referrerId) }
        assertEquals("0.00", referrerWallet.creditBalance)
        val refereeWallet = kotlinx.coroutines.runBlocking { loyalty.wallet(refereeId) }
        assertTrue(refereeWallet.referralCode.isNotBlank())

        val referral =
            kotlinx.coroutines.runBlocking {
                loyalty.claimReferral(refereeId, referrerWallet.referralCode)
            }
        assertEquals("PENDING", referral.status.name)

        transaction { loyalty.earnForPaidOrderTx(refereeId, orderId, BigDecimal("100.00")) }
        // Paid-order points are idempotent.
        transaction { loyalty.earnForPaidOrderTx(refereeId, orderId, BigDecimal("100.00")) }

        val refereeAfter = kotlinx.coroutines.runBlocking { loyalty.wallet(refereeId) }
        val referrerAfter = kotlinx.coroutines.runBlocking { loyalty.wallet(referrerId) }
        assertEquals(200, refereeAfter.pointsBalance)
        assertEquals(100, referrerAfter.pointsBalance)
        assertTrue(
            kotlinx.coroutines.runBlocking { loyalty.listReferrals(refereeId) }
                .any { it.id == referral.id && it.status.name == "COMPLETED" },
        )

        val pointsUsd = transaction { loyalty.redeemPointsTx(refereeId, 50, "checkout:$orderId") }
        assertEquals(0, pointsUsd.compareTo(BigDecimal("0.50")))
        assertEquals(150, kotlinx.coroutines.runBlocking { loyalty.wallet(refereeId) }.pointsBalance)

        kotlinx.coroutines.runBlocking {
            loyalty.adminCredit(referrerId, AdminCreditRequest(BigDecimal("25.00"), "Goodwill"))
        }
        val creditUsd = transaction { loyalty.redeemCreditTx(referrerId, BigDecimal("10.00"), "checkout:credit-$suffix") }
        assertEquals(0, creditUsd.compareTo(BigDecimal("10.00")))
        assertEquals("15.00", kotlinx.coroutines.runBlocking { loyalty.wallet(referrerId) }.creditBalance)

        val gift =
            kotlinx.coroutines.runBlocking {
                loyalty.issueGiftCard(referrerId, GiftCardIssueRequest(BigDecimal("20.00")))
            }
        val giftUsd = transaction { loyalty.redeemGiftCardTx(gift.code, refereeId, orderId, BigDecimal("7.00")) }
        assertEquals(0, giftUsd.compareTo(BigDecimal("7.00")))
        assertEquals("13.00", kotlinx.coroutines.runBlocking { loyalty.giftCard(gift.code) }.balance)

        val ledger = kotlinx.coroutines.runBlocking { loyalty.ledger(refereeId, 20, 0) }
        assertTrue(ledger.data.any { it.type.name == "POINTS_EARN" })
        assertTrue(ledger.data.any { it.type.name == "POINTS_REDEEM" })
        assertTrue(ledger.data.any { it.type.name == "REFERRAL_BONUS" })
    }
}
