package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.tax.TaxClassResponse
import com.piashcse.feature.tax.TaxExemptionResponse
import com.piashcse.feature.tax.TaxRuleResponse
import com.piashcse.feature.tax.TaxService
import com.piashcse.feature.tax.TaxZoneResponse
import com.piashcse.feature.tax.productTaxClassAdminRoutes
import com.piashcse.feature.tax.taxAdminRoutes
import com.piashcse.feature.tax.taxClassAdminRoutes
import com.piashcse.feature.tax.taxExemptionAdminRoutes
import com.piashcse.feature.tax.taxZoneAdminRoutes
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

class TaxRoutesTest {
    private val taxService: TaxService = mockk()

    private fun sample() =
        TaxRuleResponse(
            id = "tr-1",
            country = "BD",
            categoryId = null,
            zoneId = null,
            taxClassId = null,
            rate = "0.05",
            isInclusive = false,
            priority = 0,
            isActive = true,
        )

    private fun sampleClass() = TaxClassResponse("tc-1", "Reduced", "REDUCED", "0.10", isInclusive = true, isActive = true)

    private fun sampleZone() = TaxZoneResponse("tz-1", "Dhaka", "BD", "Dhaka", isNexus = true, priority = 10, isActive = true)

    private fun sampleExemption() =
        TaxExemptionResponse(
            id = "ex-1",
            userId = "user-1",
            businessTaxId = null,
            country = null,
            reason = "B2B certificate",
            certificateUrl = null,
            isActive = true,
            expiresAt = null,
            createdAt = "2026-01-01T00:00:00Z",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { taxService } })
            routing {
                route("/api/v1/admin/tax-rules") { adminAuth { taxAdminRoutes() } }
                route("/api/v1/admin/tax-classes") { adminAuth { taxClassAdminRoutes() } }
                route("/api/v1/admin/tax-zones") { adminAuth { taxZoneAdminRoutes() } }
                route("/api/v1/admin/tax-exemptions") { adminAuth { taxExemptionAdminRoutes() } }
                route("/api/v1/admin/products") { adminAuth { productTaxClassAdminRoutes() } }
            }
        }
    }

    @Test
    fun `list returns rules`() =
        testApplication {
            coEvery { taxService.list() } returns listOf(sample())
            setup()
            val res =
                client.get("/api/v1/admin/tax-rules") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "BD")
        }

    @Test
    fun `create returns 201`() =
        testApplication {
            coEvery { taxService.upsert(any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/admin/tax-rules") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"country":"BD","rate":"0.05","isActive":true}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
        }

    @Test
    fun `invalid rate returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/tax-rules") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"country":"BD","rate":"1.5"}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `delete unknown rule returns 404`() =
        testApplication {
            coEvery { taxService.delete("missing") } returns false
            setup()
            val res =
                client.delete("/api/v1/admin/tax-rules/missing") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.NotFound, res.status)
        }

    @Test
    fun `list without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/tax-rules")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `tax class create returns 201`() =
        testApplication {
            coEvery { taxService.createClass(any()) } returns sampleClass()
            setup()
            val res =
                client.post("/api/v1/admin/tax-classes") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Reduced","code":"REDUCED","rate":0.10,"isInclusive":true}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "REDUCED")
        }

    @Test
    fun `tax zone create returns 201`() =
        testApplication {
            coEvery { taxService.createZone(any()) } returns sampleZone()
            setup()
            val res =
                client.post("/api/v1/admin/tax-zones") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Dhaka","country":"BD","region":"Dhaka","priority":10}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "Dhaka")
        }

    @Test
    fun `tax exemption create returns 201`() =
        testApplication {
            coEvery { taxService.createExemption(any()) } returns sampleExemption()
            setup()
            val res =
                client.post("/api/v1/admin/tax-exemptions") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"userId":"user-1","reason":"B2B certificate"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "B2B certificate")
        }

    @Test
    fun `product tax class assignment returns 200`() =
        testApplication {
            coEvery { taxService.assignProductTaxClass("product-1", "tc-1") } returns Unit
            setup()
            val res =
                client.put("/api/v1/admin/products/product-1/tax-class") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"taxClassId":"tc-1"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }
}
