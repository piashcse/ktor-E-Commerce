package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.policy.PolicyRepository
import com.piashcse.feature.policy.policyAdminRoutes
import com.piashcse.feature.policy.policyRoutes
import com.piashcse.model.response.PolicyDocumentResponse
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

class PolicyRoutesTest {
    private val repo: PolicyRepository = mockk()

    private fun sample() =
        PolicyDocumentResponse(
            id = "p-1",
            title = "Privacy Policy",
            type = "PRIVACY_POLICY",
            content = "Policy body",
            version = "1.0",
            effectiveDate = "2024-01-01",
            isActive = true,
        )

    private fun validBody() =
        """{"title":"Privacy Policy","type":"PRIVACY_POLICY","content":"Policy body","version":"1.0","effectiveDate":"2024-01-01"}"""

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<PolicyRepository> { repo } })
            routing {
                route("/api/v1/policies") { policyRoutes() }
                route("/api/v1/admin/policies") { adminAuth { policyAdminRoutes() } }
            }
        }
    }

    @Test
    fun `public get by type happy path returns 200`() =
        testApplication {
            coEvery { repo.getPolicyByType(any()) } returns sample()
            setup()
            val res = client.get("/api/v1/policies/PRIVACY_POLICY")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Privacy Policy")
        }

    @Test
    fun `public get invalid policy type returns 400`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/policies/NOPE")
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin create happy path returns 201`() =
        testApplication {
            coEvery { repo.createPolicy(any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/admin/policies") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "Privacy Policy")
        }

    @Test
    fun `admin create without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/policies") {
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin create invalid body returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/policies") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin history happy path returns 200`() =
        testApplication {
            coEvery { repo.getAllPolicies(any()) } returns listOf(sample())
            setup()
            val res =
                client.get("/api/v1/admin/policies/PRIVACY_POLICY/history") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "1.0")
        }

    @Test
    fun `admin history without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/policies/PRIVACY_POLICY/history")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin history invalid policy type returns 400`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/policies/NOPE/history") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }
}
