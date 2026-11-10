package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.outbox.OutboxDeadLetterResponse
import com.piashcse.feature.outbox.OutboxService
import com.piashcse.feature.outbox.OutboxSummaryResponse
import com.piashcse.feature.outbox.outboxAdminRoutes
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

class OutboxRoutesTest {
    private val service: OutboxService = mockk()

    private fun deadLetter() =
        OutboxDeadLetterResponse(
            id = "dl-1",
            eventType = "OrderPlacedEvent",
            aggregateType = "ORDER",
            aggregateId = "o-1",
            payload = "{}",
            error = "smtp down",
            attempts = 5,
            createdAt = "2024-01-01T00:00:00Z",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing { route("/api/v1/admin/outbox") { adminAuth { outboxAdminRoutes() } } }
        }
    }

    @Test
    fun `summary returns counts`() =
        testApplication {
            coEvery { service.summary() } returns OutboxSummaryResponse(pending = 2, deadLetters = 1)
            setup()
            val res =
                client.get("/api/v1/admin/outbox/summary") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "pending")
        }

    @Test
    fun `dead letters are paginated`() =
        testApplication {
            coEvery { service.listDeadLetters(20, 0) } returns
                PaginatedResponse(listOf(deadLetter()), PaginationMetadata(totalCount = 1, limit = 20, offset = 0))
            setup()
            val res =
                client.get("/api/v1/admin/outbox/dead-letters") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "dl-1")
        }

    @Test
    fun `requeue acknowledges`() =
        testApplication {
            coEvery { service.requeue("dl-1") } returns Unit
            setup()
            val res =
                client.post("/api/v1/admin/outbox/dead-letters/dl-1/requeue") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "requeued")
        }

    @Test
    fun `anonymous is unauthorized`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/outbox/summary")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
