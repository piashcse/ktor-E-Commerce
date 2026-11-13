package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.WebhookEventStatus
import com.piashcse.feature.payment.WebhookResult
import com.piashcse.feature.shipping.CarrierWebhookResult
import com.piashcse.feature.webhook.WebhookAdminService
import com.piashcse.feature.webhook.WebhookEventResponse
import com.piashcse.feature.webhook.WebhookSource
import com.piashcse.feature.webhook.webhookAdminRoutes
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

class WebhookAdminRoutesTest {
    private val service: WebhookAdminService = mockk()

    private fun event() =
        WebhookEventResponse(
            id = "wh-1",
            source = WebhookSource.PAYMENT,
            provider = "STRIPE",
            eventId = "evt-1",
            eventType = "PAYMENT_SUCCEEDED",
            status = WebhookEventStatus.FAILED,
            error = "boom",
            receivedAt = "2026-06-01T00:00:00Z",
            processedAt = null,
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing { route("/api/v1/admin/webhooks") { adminAuth { webhookAdminRoutes() } } }
        }
    }

    @Test
    fun `admin lists webhook events`() =
        testApplication {
            coEvery { service.list(null, WebhookEventStatus.FAILED, 20, 0) } returns
                PaginatedResponse(listOf(event()), PaginationMetadata(1, 20, 0))
            setup()
            val res =
                client.get("/api/v1/admin/webhooks?status=FAILED") {
                    header(HttpHeaders.Authorization, authHeader(userId = "admin-1", userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "evt-1")
        }

    @Test
    fun `admin replays payment and carrier events`() =
        testApplication {
            coEvery { service.replayPayment("wh-1", "admin-1") } returns WebhookResult(true, message = "ok")
            coEvery { service.replayCarrier("wh-2", "admin-1") } returns CarrierWebhookResult(true, message = "ok")
            setup()
            val payment =
                client.post("/api/v1/admin/webhooks/payments/wh-1/replay") {
                    header(HttpHeaders.Authorization, authHeader(userId = "admin-1", userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, payment.status)
            val carrier =
                client.post("/api/v1/admin/webhooks/carriers/wh-2/replay") {
                    header(HttpHeaders.Authorization, authHeader(userId = "admin-1", userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, carrier.status)
        }

    @Test
    fun `customers cannot inspect webhook events`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/webhooks") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
