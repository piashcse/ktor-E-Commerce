package com.piashcse.feature.shipping

import com.piashcse.constants.ShipmentStatus
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShippoCarrierGatewayTest {
    private val token = "shippo_test_123"
    private val secret = "carrier_secret_123"

    private class FakeTransport(
        private val respond: (String) -> CarrierHttpResponse,
    ) : CarrierHttpTransport {
        val bodies = mutableListOf<String>()

        override suspend fun postJson(
            url: String,
            headers: Map<String, String>,
            body: String,
        ): CarrierHttpResponse {
            bodies += body
            return respond(body)
        }
    }

    private fun gateway(transport: CarrierHttpTransport) = ShippoCarrierGateway(token, secret, "https://shippo.test", transport)

    private fun quoteRequest() =
        CarrierQuoteRequest(
            orderId = "order-1",
            from =
                CarrierAddress(
                    name = "Warehouse",
                    street1 = "1 Main St",
                    city = "Dhaka",
                    zip = "1207",
                    country = "BD",
                ),
            to =
                CarrierAddress(
                    name = "Customer",
                    street1 = "2 Side St",
                    city = "Chattogram",
                    zip = "4000",
                    country = "BD",
                ),
            parcels = listOf(CarrierParcel(BigDecimal("10"), BigDecimal("8"), BigDecimal("4"), weight = BigDecimal("2"))),
        )

    @Test
    fun `quote maps carrier rates`() =
        runBlocking {
            val transport =
                FakeTransport {
                    CarrierHttpResponse(
                        200,
                        """
                        {"object_id":"shp_1","rates":[
                          {"object_id":"rate_1","provider":"USPS","servicelevel_name":"Priority",
                           "amount":"7.50","currency":"USD","estimated_days":2}
                        ]}
                        """.trimIndent(),
                    )
                }
            val rates = gateway(transport).quote(quoteRequest())

            assertEquals(1, rates.size)
            assertEquals("rate_1", rates.single().id)
            assertEquals("USPS", rates.single().carrier)
            assertEquals(BigDecimal("7.50"), rates.single().amount)
            assertEquals(2, rates.single().estimatedDays)
            assertTrue(transport.bodies.single().contains("\"address_to\""))
            assertTrue(transport.bodies.single().contains("\"weight\":\"2\""))
        }

    @Test
    fun `label purchase maps tracking and label url`() =
        runBlocking {
            val transport =
                FakeTransport {
                    CarrierHttpResponse(
                        200,
                        """
                        {"object_id":"txn_1","status":"SUCCESS","tracking_number":"9400111",
                         "label_url":"https://labels.example/txn_1.pdf",
                         "rate":{"provider":"USPS","servicelevel_name":"Priority","amount":"7.50","currency":"USD"}}
                        """.trimIndent(),
                    )
                }
            val label = gateway(transport).purchaseLabel(CarrierLabelRequest("rate_1", "order-1"))

            assertTrue(label.success)
            assertEquals("9400111", label.trackingNumber)
            assertEquals("https://labels.example/txn_1.pdf", label.labelUrl)
            assertEquals("txn_1", label.providerReference)
            assertEquals(BigDecimal("7.50"), label.amount)
        }

    @Test
    fun `label errors are surfaced`() =
        runBlocking {
            val transport =
                FakeTransport {
                    CarrierHttpResponse(
                        200,
                        """
                        {"object_id":"txn_2","status":"ERROR",
                         "messages":[{"text":"Address is invalid"}]}
                        """.trimIndent(),
                    )
                }
            val label = gateway(transport).purchaseLabel(CarrierLabelRequest("rate_1", "order-1"))

            assertFalse(label.success)
            assertEquals("Address is invalid", label.message)
        }

    @Test
    fun `webhook signature accepts hmac or the shared secret`() {
        val payload = """{"event":"track_updated"}"""
        val gateway = gateway(FakeTransport { CarrierHttpResponse(200, "{}") })
        val signature = ShippoCarrierGateway.hmacSha256(secret, payload)

        assertTrue(gateway.verifyWebhook(payload, signature))
        assertTrue(gateway.verifyWebhook(payload, secret))
        assertFalse(gateway.verifyWebhook("$payload ", signature))
        assertFalse(gateway.verifyWebhook(payload, "nope"))
        assertFalse(gateway.verifyWebhook(payload, null))
    }

    @Test
    fun `tracking webhook maps statuses`() {
        val gateway = gateway(FakeTransport { CarrierHttpResponse(200, "{}") })
        val delivered =
            gateway.parseWebhook(
                """
                {"event":"track_updated","data":{"tracking_number":"9400",
                 "tracking_status":{"status":"DELIVERED","status_details":"Delivered to door"}}}
                """.trimIndent(),
            )
        assertEquals("track_updated", delivered.eventType)
        assertEquals("9400", delivered.trackingNumber)
        assertEquals(ShipmentStatus.DELIVERED, delivered.status)
        assertEquals("Delivered to door", delivered.description)

        val transit =
            gateway.parseWebhook(
                """{"event":"track_updated","data":{"tracking_number":"9400","tracking_status":{"status":"TRANSIT"}}}""",
            )
        assertEquals(ShipmentStatus.IN_TRANSIT, transit.status)

        val unknown =
            gateway.parseWebhook("""{"event":"track_updated","data":{"tracking_number":"9400"}}""")
        assertNull(unknown.status)
    }

    @Test
    fun `manual carrier cannot quote or buy labels`() =
        runBlocking {
            val gateway = ManualCarrierGateway()
            assertEquals("MANUAL", gateway.provider)
            assertTrue(gateway.quote(quoteRequest()).isEmpty())
            assertFalse(gateway.purchaseLabel(CarrierLabelRequest("rate", "order")).success)
            assertFalse(gateway.verifyWebhook("{}", "sig"))
        }
}
