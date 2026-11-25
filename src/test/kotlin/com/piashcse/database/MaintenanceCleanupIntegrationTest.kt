package com.piashcse.database

import com.piashcse.constants.AnalyticsEventType
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.UserType
import com.piashcse.database.entities.AnalyticsEventDAO
import com.piashcse.database.entities.CarrierWebhookEventDAO
import com.piashcse.database.entities.CarrierWebhookEventTable
import com.piashcse.database.entities.PaymentWebhookEventDAO
import com.piashcse.database.entities.SupportTicketDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.service.MaintenanceCleanup
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Retention job against PostgreSQL. */
class MaintenanceCleanupIntegrationTest {
    @Test
    fun `retention cleanup removes old operational rows`() {
        PostgresTestSupport.withDatabase { jdbcUrl, user, password ->
            runCleanup(jdbcUrl, user, password)
        }
    }

    private fun runCleanup(
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
        lateinit var oldWebhook: String
        lateinit var newWebhook: String
        lateinit var oldAnalytics: String
        lateinit var newAnalytics: String
        lateinit var oldTicket: String

        transaction {
            val customer =
                UserDAO.new {
                    email = "retention-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            oldWebhook =
                PaymentWebhookEventDAO.new {
                    provider = PaymentProvider.STRIPE
                    eventId = "old-$suffix"
                    eventType = "payment_intent.succeeded"
                    payload = "{}"
                    receivedAt = currentUtc().minusDays(120)
                }.id.value
            newWebhook =
                PaymentWebhookEventDAO.new {
                    provider = PaymentProvider.STRIPE
                    eventId = "new-$suffix"
                    eventType = "payment_intent.succeeded"
                    payload = "{}"
                    receivedAt = currentUtc().minusDays(10)
                }.id.value
            CarrierWebhookEventDAO.new {
                provider = "shippo"
                eventId = "old-carrier-$suffix"
                eventType = "track_updated"
                payload = "{}"
                receivedAt = currentUtc().minusDays(120)
            }
            oldAnalytics =
                AnalyticsEventDAO.new {
                    sessionId = "old-session-$suffix"
                    eventType = AnalyticsEventType.PAGE_VIEW
                    occurredAt = currentUtc().minusMonths(14)
                }.id.value
            newAnalytics =
                AnalyticsEventDAO.new {
                    sessionId = "new-session-$suffix"
                    eventType = AnalyticsEventType.PAGE_VIEW
                    occurredAt = currentUtc().minusMonths(2)
                }.id.value
            oldTicket =
                SupportTicketDAO.new {
                    userId = customer.id
                    subject = "Old ticket $suffix"
                    resolvedAt = currentUtc().minusYears(4)
                }.id.value
        }

        kotlinx.coroutines.runBlocking { MaintenanceCleanup.clean() }

        transaction {
            assertEquals(null, PaymentWebhookEventDAO.findById(oldWebhook))
            assertTrue(PaymentWebhookEventDAO.findById(newWebhook) != null)
            assertEquals(null, AnalyticsEventDAO.findById(oldAnalytics))
            assertTrue(AnalyticsEventDAO.findById(newAnalytics) != null)
            assertEquals(null, SupportTicketDAO.findById(oldTicket))
            assertEquals(
                0,
                CarrierWebhookEventDAO.find {
                    CarrierWebhookEventTable.eventId eq "old-carrier-$suffix"
                }.count().toInt(),
            )
        }
    }
}
