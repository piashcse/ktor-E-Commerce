package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.tax.TaxRuleResponse
import com.piashcse.feature.tax.TaxService
import com.piashcse.feature.tax.taxAdminRoutes
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

    private fun sample() = TaxRuleResponse(id = "tr-1", country = "BD", categoryId = null, rate = "0.05", isActive = true)

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { taxService } })
            routing { route("/api/v1/admin/tax-rules") { adminAuth { taxAdminRoutes() } } }
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
}
