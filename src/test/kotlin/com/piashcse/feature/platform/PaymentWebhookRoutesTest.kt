package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.payment.PaymentRepository
import com.piashcse.feature.payment.WebhookResult
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.utils.validator.ValidationException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class PaymentWebhookRoutesTest {
    private val repo: PaymentRepository = mockk(relaxed = true)

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { repo } })
            routing { route("/api/v1/payments") { paymentRoutes() } }
        }
    }

    @Test
    fun `provider webhook forwards the raw payload and signature`() =
        testApplication {
            coEvery { repo.handleWebhook("stripe", any(), "t=1,v1=abc") } returns
                WebhookResult(true, message = "Webhook processed")
            setup()

            val res =
                client.post("/api/v1/payments/webhook/stripe") {
                    header("Stripe-Signature", "t=1,v1=abc")
                    contentType(ContentType.Application.Json)
                    setBody("""{"id":"evt_1"}""")
                }

            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Webhook processed")
            coVerify(exactly = 1) { repo.handleWebhook("stripe", """{"id":"evt_1"}""", "t=1,v1=abc") }
        }

    @Test
    fun `generic webhook signature header is accepted`() =
        testApplication {
            coEvery { repo.handleWebhook("generic", any(), "sig") } returns
                WebhookResult(true, message = "Webhook processed")
            setup()

            val res =
                client.post("/api/v1/payments/webhook/generic") {
                    header("X-Webhook-Signature", "sig")
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }

            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `invalid signature is rejected by the repository`() =
        testApplication {
            coEvery { repo.handleWebhook(any(), any(), any()) } throws ValidationException("Invalid webhook signature")
            setup()

            val res =
                client.post("/api/v1/payments/webhook/stripe") {
                    header("Stripe-Signature", "bad")
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }

            assertEquals(HttpStatusCode.BadRequest, res.status)
        }
}
