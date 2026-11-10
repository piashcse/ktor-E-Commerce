package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.ShipmentStatus
import com.piashcse.feature.shipping.CarrierRate
import com.piashcse.feature.shipping.CarrierWebhookResult
import com.piashcse.feature.shipping.ShipmentResponse
import com.piashcse.feature.shipping.ShipmentService
import com.piashcse.feature.shipping.orderShipmentRoutes
import com.piashcse.feature.shipping.shipmentRoutes
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
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ShipmentRoutesTest {
    private val service: ShipmentService = mockk()

    private fun sampleShipment() =
        ShipmentResponse(
            id = "ship-1",
            orderId = "order-1",
            carrier = "USPS",
            service = "Priority",
            trackingNumber = "9400",
            labelUrl = "https://labels.example/1.pdf",
            rateAmount = "7.50",
            currency = "USD",
            status = ShipmentStatus.LABEL_PURCHASED,
            shippedAt = "2026-01-01T00:00:00Z",
            deliveredAt = null,
            createdAt = "2026-01-01T00:00:00Z",
            items = emptyList(),
        )

    private val sampleRate =
        CarrierRate(
            id = "rate-1",
            carrier = "USPS",
            service = "Priority",
            amount = BigDecimal("7.50"),
            currency = "USD",
            estimatedDays = 2,
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing {
                route("/api/v1/orders") { orderShipmentRoutes() }
                route("/api/v1/shipments") { shipmentRoutes() }
            }
        }
    }

    @Test
    fun `quote returns carrier rates`() =
        testApplication {
            coEvery { service.quote("order-1", "user-1", any()) } returns listOf(sampleRate)
            setup()
            val res =
                client.post("/api/v1/orders/order-1/shipping-quotes") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {"origin":{"name":"Shop","streetAddress":"1 Main","city":"Dhaka","zipCode":"1207","country":"BD"},
                         "destination":{"name":"Customer","streetAddress":"2 Side","city":"Chattogram","zipCode":"4000","country":"BD"},
                         "parcels":[{"length":10,"width":8,"height":4,"weight":2}]}
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "rate-1")
        }

    @Test
    fun `label purchase returns 201`() =
        testApplication {
            coEvery { service.purchaseLabel("order-1", "user-1", any()) } returns sampleShipment()
            setup()
            val res =
                client.post("/api/v1/orders/order-1/shipments") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"rateId":"rate-1"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "ship-1")
        }

    @Test
    fun `carrier webhook forwards the raw payload and signature`() =
        testApplication {
            coEvery { service.handleWebhook("shippo", any(), "sig") } returns
                CarrierWebhookResult(true, message = "Webhook processed")
            setup()
            val res =
                client.post("/api/v1/shipments/webhook/shippo") {
                    header("X-Carrier-Signature", "sig")
                    contentType(ContentType.Application.Json)
                    setBody("""{"event":"track_updated"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            coVerify(exactly = 1) { service.handleWebhook("shippo", """{"event":"track_updated"}""", "sig") }
        }

    @Test
    fun `invalid webhook signature is rejected`() =
        testApplication {
            coEvery { service.handleWebhook(any(), any(), any()) } throws
                ValidationException("Invalid carrier webhook signature")
            setup()
            val res =
                client.post("/api/v1/shipments/webhook/shippo") {
                    header("X-Carrier-Signature", "bad")
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `anonymous users cannot read shipments`() =
        testApplication {
            setup()
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/shipments/ship-1").status)
        }
}
