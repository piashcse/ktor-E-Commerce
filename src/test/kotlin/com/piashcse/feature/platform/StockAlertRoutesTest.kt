package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.stockalert.StockAlertResponse
import com.piashcse.feature.stockalert.StockAlertService
import com.piashcse.feature.stockalert.stockAlertRoutes
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

class StockAlertRoutesTest {
    private val service: StockAlertService = mockk()

    private fun sample() =
        StockAlertResponse(
            id = "alert-1",
            productId = "prod-1",
            variantId = null,
            notifiedAt = null,
            createdAt = "2026-01-01T00:00:00Z",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing { route("/api/v1/stock-alerts") { stockAlertRoutes() } }
        }
    }

    @Test
    fun `customer subscribes to a stock alert`() =
        testApplication {
            coEvery { service.subscribe("user-1", any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/stock-alerts") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"productId":"prod-1"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "prod-1")
        }

    @Test
    fun `customer lists stock alerts`() =
        testApplication {
            coEvery { service.list("user-1") } returns listOf(sample())
            setup()
            val res =
                client.get("/api/v1/stock-alerts") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "alert-1")
        }

    @Test
    fun `customer removes a stock alert`() =
        testApplication {
            coEvery { service.unsubscribe("user-1", "prod-1") } returns true
            setup()
            val res =
                client.delete("/api/v1/stock-alerts/prod-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "removed")
        }

    @Test
    fun `anonymous is unauthorized`() =
        testApplication {
            setup()
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/stock-alerts").status)
        }
}
