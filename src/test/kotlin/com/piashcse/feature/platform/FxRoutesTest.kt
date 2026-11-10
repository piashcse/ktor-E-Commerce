package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.fx.FxRateResponse
import com.piashcse.feature.fx.fxAdminRoutes
import com.piashcse.plugin.adminAuth
import com.piashcse.service.FxService
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

class FxRoutesTest {
    private val fxService: FxService = mockk()

    private fun sample() = FxRateResponse(targetCurrency = "EUR", rate = "0.92", effectiveAt = "2024-01-01T00:00:00")

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { fxService } })
            routing { route("/api/v1/admin/fx-rates") { adminAuth { fxAdminRoutes() } } }
        }
    }

    @Test
    fun `list returns rates`() =
        testApplication {
            coEvery { fxService.listRates() } returns listOf(sample())
            setup()
            val res =
                client.get("/api/v1/admin/fx-rates") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "EUR")
        }

    @Test
    fun `unsupported currency returns 400`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/admin/fx-rates/XYZ") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"rate":"1.0"}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `upsert returns the stored rate`() =
        testApplication {
            coEvery { fxService.upsertRate("EUR", any()) } returns sample()
            setup()
            val res =
                client.put("/api/v1/admin/fx-rates/EUR") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"rate":"0.92"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "0.92")
        }

    @Test
    fun `non admin is forbidden`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/fx-rates") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
