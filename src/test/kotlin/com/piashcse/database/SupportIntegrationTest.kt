@file:Suppress("LongMethod")

package com.piashcse.database

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.TicketCategory
import com.piashcse.constants.TicketPriority
import com.piashcse.constants.TicketStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.feature.support.CreateTicketRequest
import com.piashcse.feature.support.SupportService
import com.piashcse.feature.support.TicketReplyRequest
import com.piashcse.feature.support.TicketSatisfactionRequest
import com.piashcse.feature.support.TicketUpdateRequest
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Support tickets, replies and internal notes against PostgreSQL. */
class SupportIntegrationTest {
    @Test
    fun `tickets support replies internal notes and resolution`() {
        PostgresTestSupport.withDatabase { jdbcUrl, user, password ->
            runSupportFlow(jdbcUrl, user, password)
        }
    }

    private fun runSupportFlow(
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
        lateinit var adminId: String
        lateinit var orderId: String

        transaction {
            val customer =
                UserDAO.new {
                    email = "support-customer-$suffix@example.com"
                    userType = UserType.CUSTOMER
                    password = "hashed"
                    isVerified = true
                }
            val admin =
                UserDAO.new {
                    email = "support-admin-$suffix@example.com"
                    userType = UserType.ADMIN
                    password = "hashed"
                    isVerified = true
                }
            customerId = customer.id.value
            adminId = admin.id.value
            val order =
                OrderDAO.new {
                    userId = customer.id
                    orderNumber = "SUP-$suffix"
                    subTotal = BigDecimal("10.00")
                    total = BigDecimal("10.00")
                    status = OrderStatus.PAID
                    paymentStatus = PaymentStatus.COMPLETED
                }
            orderId = order.id.value
        }

        val support = SupportService()
        val created =
            kotlinx.coroutines.runBlocking {
                support.create(
                    customerId,
                    CreateTicketRequest(
                        subject = "Missing item",
                        body = "One item was missing from the parcel",
                        category = TicketCategory.ORDER,
                        orderId = orderId,
                    ),
                )
            }
        assertEquals(TicketStatus.OPEN, created.ticket.status)
        assertEquals(1, created.messages.size)
        val attachment =
            kotlinx.coroutines.runBlocking {
                support.attach(
                    customerId,
                    created.ticket.id,
                    fileName = "proof.png",
                    contentType = "image/png",
                    storageName = "stored-$suffix.png",
                    sizeBytes = 4,
                )
            }
        assertEquals("proof.png", attachment.fileName)
        val withAttachment = kotlinx.coroutines.runBlocking { support.myTicket(customerId, created.ticket.id) }
        assertEquals(1, withAttachment.attachments.size)
        assertEquals(
            "stored-$suffix.png",
            kotlinx.coroutines.runBlocking {
                support.attachmentForCustomer(customerId, created.ticket.id, attachment.id)
            }.storageName,
        )

        val customerReply =
            kotlinx.coroutines.runBlocking {
                support.reply(customerId, created.ticket.id, TicketReplyRequest("Any update?"))
            }
        assertEquals(2, customerReply.messages.size)

        val internal =
            kotlinx.coroutines.runBlocking {
                support.adminReply(adminId, created.ticket.id, TicketReplyRequest("Checking with carrier", isInternal = true))
            }
        assertEquals(3, internal.messages.size)
        val publicReply =
            kotlinx.coroutines.runBlocking {
                support.adminReply(adminId, created.ticket.id, TicketReplyRequest("We are reshipping it"))
            }
        assertEquals(4, publicReply.messages.size)

        // Customers never see internal notes.
        val customerView = kotlinx.coroutines.runBlocking { support.myTicket(customerId, created.ticket.id) }
        assertEquals(3, customerView.messages.size)
        assertTrue(customerView.messages.none { it.isInternal })

        val updated =
            kotlinx.coroutines.runBlocking {
                support.adminUpdate(
                    created.ticket.id,
                    TicketUpdateRequest(
                        status = TicketStatus.RESOLVED,
                        priority = TicketPriority.HIGH,
                        assignedTo = adminId,
                    ),
                )
            }
        assertEquals(TicketStatus.RESOLVED, updated.status)
        assertEquals(TicketPriority.HIGH, updated.priority)
        assertEquals(adminId, updated.assignedTo)
        assertTrue(updated.resolvedAt != null)

        val closed = kotlinx.coroutines.runBlocking { support.close(customerId, created.ticket.id) }
        assertEquals(TicketStatus.CLOSED, closed.status)
        assertTrue(closed.firstResponseAt != null)
        assertTrue(closed.responseDueAt != null)
        val rated =
            kotlinx.coroutines.runBlocking {
                support.rate(customerId, created.ticket.id, TicketSatisfactionRequest(5, "Fast resolution"))
            }
        assertEquals(5, rated.csatScore)
        assertEquals("Fast resolution", rated.csatComment)
        assertTrue(rated.csatRatedAt != null)

        assertEquals(1, kotlinx.coroutines.runBlocking { support.myTickets(customerId, 10, 0) }.data.size)
        assertEquals(
            1,
            kotlinx.coroutines.runBlocking { support.adminTickets(null, null, null, 10, 0) }.data
                .count { it.id == created.ticket.id },
        )
    }
}
