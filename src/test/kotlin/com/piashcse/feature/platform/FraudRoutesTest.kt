package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.ChargebackStatus
import com.piashcse.constants.FraudMetric
import com.piashcse.constants.RiskAction
import com.piashcse.feature.fraud.BlocklistResponse
import com.piashcse.feature.fraud.ChargebackResponse
import com.piashcse.feature.fraud.ChargebackService
import com.piashcse.feature.fraud.FraudRuleResponse
import com.piashcse.feature.fraud.FraudService
import com.piashcse.feature.fraud.chargebackAdminRoutes
import com.piashcse.feature.fraud.fraudAdminRoutes
import com.piashcse.feature.fraud.paymentRiskAdminRoutes
import com.piashcse.plugin.adminAuth
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

class FraudRoutesTest {
    private val fraudService: FraudService = mockk()
    private val chargebackService: ChargebackService = mockk()

    private fun rule() = FraudRuleResponse("rule-1", "Velocity", FraudMetric.ORDER_COUNT, 60, "5.00", RiskAction.REVIEW, 1, true)

    private fun blocklist() = BlocklistResponse("bl-1", com.piashcse.constants.BlocklistType.USER, "user-1", "fraud", true, null, null)

    private fun chargeback() =
        ChargebackResponse(
            id = "cb-1",
            paymentId = "pay-1",
            orderId = "order-1",
            providerDisputeId = "dp-1",
            amount = "10.00",
            currency = "USD",
            status = ChargebackStatus.OPEN,
            reason = "fraudulent",
            outcome = null,
            evidenceDueAt = null,
            openedAt = "2026-01-01T00:00:00Z",
            resolvedAt = null,
            createdAt = "2026-01-01T00:00:00Z",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single { fraudService }
                    single { chargebackService }
                },
            )
            routing {
                route("/api/v1/admin/fraud") { adminAuth { fraudAdminRoutes() } }
                route("/api/v1/admin/payments") { adminAuth { paymentRiskAdminRoutes() } }
                route("/api/v1/admin/chargebacks") { adminAuth { chargebackAdminRoutes() } }
            }
        }
    }

    @Test
    fun `admin lists fraud rules`() =
        testApplication {
            coEvery { fraudService.listRules() } returns listOf(rule())
            setup()
            val res =
                client.get("/api/v1/admin/fraud/rules") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "rule-1")
        }

    @Test
    fun `admin creates a fraud rule`() =
        testApplication {
            coEvery { fraudService.createRule(any()) } returns rule()
            setup()
            val res =
                client.post("/api/v1/admin/fraud/rules") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Velocity","metric":"ORDER_COUNT","windowMinutes":60,"threshold":5,"action":"REVIEW"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
        }

    @Test
    fun `admin adds a blocklist entry`() =
        testApplication {
            coEvery { fraudService.createBlocklistEntry(any()) } returns blocklist()
            setup()
            val res =
                client.post("/api/v1/admin/fraud/blocklist") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"type":"USER","value":"user-1","reason":"fraud"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "bl-1")
        }

    @Test
    fun `admin records AVS and CVV results`() =
        testApplication {
            coEvery { fraudService.recordRiskResult("pay-1", any()) } returns Unit
            setup()
            val res =
                client.post("/api/v1/admin/payments/pay-1/risk") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"avsStatus":"MATCH","cvvStatus":"MISMATCH"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `admin creates a chargeback`() =
        testApplication {
            coEvery { chargebackService.create(any()) } returns chargeback()
            setup()
            val res =
                client.post("/api/v1/admin/chargebacks") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"paymentId":"pay-1","providerDisputeId":"dp-1","reason":"fraudulent"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "cb-1")
        }

    @Test
    fun `customers cannot access fraud admin routes`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/fraud/rules") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
