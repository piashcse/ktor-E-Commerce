package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.SupportSenderRole
import com.piashcse.constants.TicketCategory
import com.piashcse.constants.TicketPriority
import com.piashcse.constants.TicketStatus
import com.piashcse.feature.support.SupportService
import com.piashcse.feature.support.TicketMessageResponse
import com.piashcse.feature.support.TicketResponse
import com.piashcse.feature.support.TicketSummaryResponse
import com.piashcse.feature.support.supportAdminRoutes
import com.piashcse.feature.support.supportRoutes
import com.piashcse.plugin.adminAuth
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SupportRoutesTest {
    private val service: SupportService = mockk()

    private fun summary() =
        TicketSummaryResponse(
            "ticket-1", "user-1", null, null, "Where is my order?", TicketCategory.ORDER,
            TicketPriority.NORMAL, TicketStatus.OPEN, null, null, null, null,
        )

    private fun ticket() =
        TicketResponse(
            summary(),
            listOf(
                TicketMessageResponse(
                    "msg-1",
                    "ticket-1",
                    "user-1",
                    SupportSenderRole.CUSTOMER,
                    "Where is my order?",
                    false,
                    null,
                ),
            ),
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing {
                route("/api/v1/support/tickets") { supportRoutes() }
                route("/api/v1/admin/support/tickets") { adminAuth { supportAdminRoutes() } }
            }
        }
    }

    @Test
    fun `customer opens a ticket`() =
        testApplication {
            coEvery { service.create("user-1", any()) } returns ticket()
            setup()
            val res =
                client.post("/api/v1/support/tickets") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"subject":"Where is my order?","body":"Order is late"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "ticket-1")
        }

    @Test
    fun `customer lists tickets`() =
        testApplication {
            coEvery { service.myTickets("user-1", 20, 0) } returns
                PaginatedResponse(listOf(summary()), PaginationMetadata(1, 20, 0))
            setup()
            val res =
                client.get("/api/v1/support/tickets") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Where is my order?")
        }

    @Test
    fun `customer replies and closes`() =
        testApplication {
            coEvery { service.reply("user-1", "ticket-1", any()) } returns ticket()
            coEvery { service.close("user-1", "ticket-1") } returns summary().copy(status = TicketStatus.CLOSED)
            setup()
            val reply =
                client.post("/api/v1/support/tickets/ticket-1/messages") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"body":"Any update?"}""")
                }
            assertEquals(HttpStatusCode.OK, reply.status)
            val closed =
                client.post("/api/v1/support/tickets/ticket-1/close") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, closed.status)
            assertContains(closed.bodyAsText(), "CLOSED")
        }

    @Test
    fun `customer rates a resolved ticket`() =
        testApplication {
            coEvery { service.rate("user-1", "ticket-1", any()) } returns
                summary().copy(status = TicketStatus.CLOSED, csatScore = 5, csatComment = "Great")
            setup()
            val res =
                client.post("/api/v1/support/tickets/ticket-1/satisfaction") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"score":5,"comment":"Great"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "\"csatScore\": 5")
        }

    @Test
    fun `admin replies internally and updates status`() =
        testApplication {
            coEvery { service.adminReply("admin-1", "ticket-1", any()) } returns ticket()
            coEvery { service.adminUpdate("ticket-1", any(), "admin-1") } returns summary().copy(status = TicketStatus.RESOLVED)
            setup()
            val reply =
                client.post("/api/v1/admin/support/tickets/ticket-1/messages") {
                    header(HttpHeaders.Authorization, authHeader(userId = "admin-1", userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"body":"Checking with carrier","isInternal":true}""")
                }
            assertEquals(HttpStatusCode.OK, reply.status)
            val updated =
                client.put("/api/v1/admin/support/tickets/ticket-1") {
                    header(HttpHeaders.Authorization, authHeader(userId = "admin-1", userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"RESOLVED","priority":"HIGH"}""")
                }
            assertEquals(HttpStatusCode.OK, updated.status)
            assertContains(updated.bodyAsText(), "RESOLVED")
        }

    @Test
    fun `customers cannot access admin queue`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/support/tickets") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
