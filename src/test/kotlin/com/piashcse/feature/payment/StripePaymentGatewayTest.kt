package com.piashcse.feature.payment

import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.ScaStatus
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StripePaymentGatewayTest {
    private val secretKey = "sk_test_123"
    private val webhookSecret = "whsec_test_123"
    private val now = 1_700_000_000_000L

    private class FakeTransport(
        private val respond: (Map<String, String>) -> GatewayHttpResponse,
    ) : HttpTransport {
        val requests = mutableListOf<Pair<String, Map<String, String>>>()

        override suspend fun postForm(
            url: String,
            headers: Map<String, String>,
            form: Map<String, String>,
        ): GatewayHttpResponse {
            requests += url to form
            return respond(form)
        }
    }

    private fun gateway(transport: HttpTransport) =
        StripePaymentGateway(
            secretKey = secretKey,
            webhookSecret = webhookSecret,
            baseUrl = "https://stripe.test",
            transport = transport,
            clock = { now },
        )

    @Test
    fun `charge returns client secret and 3DS action`() =
        runBlocking {
            val transport =
                FakeTransport {
                    GatewayHttpResponse(
                        200,
                        """
                        {"id":"pi_123","status":"requires_action","client_secret":"pi_123_secret",
                         "next_action":{"redirect_to_url":{"url":"https://3ds.example/redirect"}}}
                        """.trimIndent(),
                    )
                }
            val result =
                gateway(transport).charge(
                    orderId = "order-1",
                    amount = BigDecimal("25.00"),
                    currency = "usd",
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    transactionId = "tx-1",
                    returnUrl = "https://shop.example/return",
                    idempotencyKey = "charge-1",
                )

            assertTrue(result.success)
            assertFalse(result.captured)
            assertEquals("pi_123", result.gatewayTransactionId)
            assertEquals("pi_123_secret", result.clientSecret)
            assertEquals("https://3ds.example/redirect", result.nextActionUrl)
            assertEquals(ScaStatus.REQUIRED, result.scaStatus)
            assertEquals("2500", transport.requests.single().second["amount"])
            assertEquals("usd", transport.requests.single().second["currency"])
            assertEquals("tx-1", transport.requests.single().second["metadata[transaction_id]"])
        }

    @Test
    fun `charge reports an immediate capture`() =
        runBlocking {
            val transport =
                FakeTransport {
                    GatewayHttpResponse(200, """{"id":"pi_ok","status":"succeeded","client_secret":"pi_ok_secret"}""")
                }
            val result =
                gateway(transport).charge("order-1", BigDecimal("10.00"), "USD", PaymentMethod.CREDIT_CARD, null)

            assertTrue(result.success)
            assertTrue(result.captured)
            assertEquals(ScaStatus.AUTHENTICATED, result.scaStatus)
        }

    @Test
    fun `charge surfaces provider errors`() =
        runBlocking {
            val transport =
                FakeTransport {
                    GatewayHttpResponse(402, """{"error":{"message":"Your card was declined."}}""")
                }
            val result =
                gateway(transport).charge("order-1", BigDecimal("10.00"), "USD", PaymentMethod.CREDIT_CARD, null)

            assertFalse(result.success)
            assertEquals("Your card was declined.", result.message)
        }

    @Test
    fun `refund returns the provider refund id`() =
        runBlocking {
            val transport =
                FakeTransport {
                    GatewayHttpResponse(200, """{"id":"re_1","status":"succeeded","amount":2500,"currency":"usd"}""")
                }
            val result = gateway(transport).refund("pi_123", BigDecimal("25.00"), "usd", "refund:req-1")

            assertTrue(result.success)
            assertEquals("re_1", result.providerRefundId)
            assertEquals("succeeded", result.status)
            assertEquals("2500", transport.requests.single().second["amount"])
        }

    @Test
    fun `webhook signature is verified against the raw payload`() {
        val payload = """{"id":"evt_1","type":"payment_intent.succeeded"}"""
        val timestamp = now / 1000
        val signature = StripePaymentGateway.hmacSha256(webhookSecret, "$timestamp.$payload")
        val gateway = gateway(FakeTransport { GatewayHttpResponse(200, "{}") })

        assertTrue(gateway.verifyWebhook(payload, "t=$timestamp,v1=$signature"))
        assertFalse(gateway.verifyWebhook("$payload ", "t=$timestamp,v1=$signature"))
        assertFalse(gateway.verifyWebhook(payload, "t=$timestamp,v1=deadbeef"))
        assertFalse(gateway.verifyWebhook(payload, "t=1,v1=$signature"))
        assertFalse(gateway.verifyWebhook(payload, null))
    }

    @Test
    fun `parses payment and refund webhook events`() {
        val gateway = gateway(FakeTransport { GatewayHttpResponse(200, "{}") })

        val succeeded =
            gateway.parseWebhook(
                """
                {"id":"evt_1","type":"payment_intent.succeeded",
                 "data":{"object":{"id":"pi_123","status":"succeeded","amount_received":2500,"currency":"usd"}}}
                """.trimIndent(),
            )
        assertEquals(GatewayEventType.PAYMENT_SUCCEEDED, succeeded.type)
        assertEquals("pi_123", succeeded.providerReference)
        assertEquals(BigDecimal("25.00"), succeeded.amount)
        assertEquals(ScaStatus.AUTHENTICATED, succeeded.scaStatus)

        val failed =
            gateway.parseWebhook(
                """
                {"id":"evt_2","type":"payment_intent.payment_failed",
                 "data":{"object":{"id":"pi_124","status":"requires_payment_method","amount":2500,"currency":"usd"}}}
                """.trimIndent(),
            )
        assertEquals(GatewayEventType.PAYMENT_FAILED, failed.type)
        assertEquals(ScaStatus.FAILED, failed.scaStatus)

        val refund =
            gateway.parseWebhook(
                """
                {"id":"evt_3","type":"refund.updated",
                 "data":{"object":{"id":"re_1","payment_intent":"pi_123","status":"succeeded","amount":2500,"currency":"usd"}}}
                """.trimIndent(),
            )
        assertEquals(GatewayEventType.REFUND_UPDATED, refund.type)
        assertEquals("re_1", refund.refund?.providerRefundId)
        assertEquals("succeeded", refund.refund?.status)
        assertEquals("pi_123", refund.providerReference)
    }

    @Test
    fun `subscription renewal charges the stored payment method off session`() =
        runBlocking {
            val transport =
                FakeTransport {
                    GatewayHttpResponse(200, """{"id":"pi_renew","status":"succeeded"}""")
                }
            val result =
                gateway(transport).chargeSubscription(
                    subscriptionId = "sub-1",
                    invoiceId = "inv-1",
                    amount = BigDecimal("9.99"),
                    currency = "USD",
                    providerCustomerReference = "cus_1",
                    paymentMethodReference = "pm_1",
                    idempotencyKey = "renew:inv-1:1",
                )

            assertTrue(result.success)
            assertTrue(result.captured)
            assertEquals("pi_renew", result.gatewayTransactionId)
            val form = transport.requests.single().second
            assertEquals("999", form["amount"])
            assertEquals("pm_1", form["payment_method"])
            assertEquals("cus_1", form["customer"])
            assertEquals("true", form["off_session"])
            assertEquals("true", form["confirm"])
            assertEquals("sub-1", form["metadata[subscription_id]"])
            assertEquals("inv-1", form["metadata[subscription_invoice_id]"])
        }

    @Test
    fun `subscription webhook exposes invoice metadata`() {
        val gateway = gateway(FakeTransport { GatewayHttpResponse(200, "{}") })
        val event =
            gateway.parseWebhook(
                """
                {"id":"evt_sub","type":"payment_intent.succeeded",
                 "data":{"object":{"id":"pi_sub","status":"succeeded","amount_received":999,"currency":"usd",
                   "metadata":{"subscription_id":"sub-1","subscription_invoice_id":"inv-1"}}}}
                """.trimIndent(),
            )
        assertEquals("inv-1", event.metadata["subscription_invoice_id"])
        assertEquals("sub-1", event.metadata["subscription_id"])
    }

    @Test
    fun `unknown events do not mutate anything`() {
        val gateway = gateway(FakeTransport { GatewayHttpResponse(200, "{}") })
        val event =
            gateway.parseWebhook(
                """{"id":"evt_unknown","type":"customer.created","data":{"object":{"id":"cus_1"}}}""",
            )
        assertEquals(GatewayEventType.UNKNOWN, event.type)
        assertNull(event.providerReference)
    }

    @Test
    fun `manual gateway only supports cash on delivery`() =
        runBlocking {
            val gateway = ManualPaymentGateway()
            assertEquals(PaymentProvider.MANUAL, gateway.provider)
            assertTrue(gateway.supports(PaymentMethod.COD))
            assertFalse(gateway.supports(PaymentMethod.CREDIT_CARD))
            assertFalse(gateway.verifyWebhook("{}", "sig"))
            assertFalse(gateway.refund("pi_1", BigDecimal.ONE, "USD", "refund:1").success)
        }
}
