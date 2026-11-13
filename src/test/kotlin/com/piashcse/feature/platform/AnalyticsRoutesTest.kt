package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.analytics.AnalyticsIngestResponse
import com.piashcse.feature.analytics.AnalyticsService
import com.piashcse.feature.analytics.AttributionResponse
import com.piashcse.feature.analytics.FunnelResponse
import com.piashcse.feature.analytics.FunnelStage
import com.piashcse.feature.analytics.ProductFunnelResponse
import com.piashcse.feature.analytics.analyticsAdminRoutes
import com.piashcse.feature.analytics.analyticsRoutes
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

class AnalyticsRoutesTest {
    private val service: AnalyticsService = mockk()

    private fun funnel() =
        FunnelResponse(
            startDate = "2026-05-01",
            endDate = "2026-05-31",
            stages = listOf(FunnelStage("page_view", 10, null), FunnelStage("purchase", 2, 20.0)),
            overallConversion = 20.0,
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing {
                route("/api/v1/analytics") { analyticsRoutes() }
                route("/api/v1/admin/analytics") { adminAuth { analyticsAdminRoutes() } }
            }
        }
    }

    @Test
    fun `events are accepted anonymously`() =
        testApplication {
            coEvery { service.track(null, any()) } returns AnalyticsIngestResponse(1)
            setup()
            val res =
                client.post("/api/v1/analytics/events") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"consent":true,"events":[{"eventType":"PAGE_VIEW","sessionId":"s-1","path":"/"}]}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "\"accepted\": 1")
        }

    @Test
    fun `events are rejected without consent`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/analytics/events") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"consent":false,"events":[{"eventType":"PAGE_VIEW","sessionId":"s-1"}]}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "\"accepted\": 0")
            assertContains(res.bodyAsText(), "\"rejected\": 1")
        }

    @Test
    fun `bot traffic is rejected`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/analytics/events") {
                    header(HttpHeaders.UserAgent, "Googlebot/2.1")
                    contentType(ContentType.Application.Json)
                    setBody("""{"events":[{"eventType":"PAGE_VIEW","sessionId":"s-1"}]}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "\"rejected\": 1")
        }

    @Test
    fun `admin reads the funnel`() =
        testApplication {
            coEvery { service.funnel(null, null) } returns funnel()
            setup()
            val res =
                client.get("/api/v1/admin/analytics/funnel") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "page_view")
        }

    @Test
    fun `admin reads attribution`() =
        testApplication {
            coEvery { service.attribution("2026-05-01", "2026-05-31") } returns
                AttributionResponse(
                    startDate = "2026-05-01",
                    endDate = "2026-05-31",
                    rows = emptyList(),
                    totalOrders = 0,
                    totalRevenueUsd = "0.00",
                )
            setup()
            val res =
                client.get("/api/v1/admin/analytics/attribution?startDate=2026-05-01&endDate=2026-05-31") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `admin reads product conversion`() =
        testApplication {
            coEvery { service.productFunnel(null, null, 20) } returns
                ProductFunnelResponse("2026-05-01", "2026-05-31", emptyList())
            setup()
            val res =
                client.get("/api/v1/admin/analytics/products") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `customers cannot read admin analytics`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/analytics/funnel") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
