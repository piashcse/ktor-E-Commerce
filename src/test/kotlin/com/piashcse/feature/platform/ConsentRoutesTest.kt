package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.consent.ConsentRepository
import com.piashcse.feature.consent.consentRoutes
import com.piashcse.model.response.UserPolicyConsentResponse
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

class ConsentRoutesTest {
    private val repo: ConsentRepository = mockk()

    private fun sample() =
        UserPolicyConsentResponse(
            id = "uc-1",
            userId = "user-1",
            policyId = "pol-1",
            consentDate = "2024-01-01T00:00:00",
            ipAddress = null,
            userAgent = null,
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<ConsentRepository> { repo } })
            routing { route("/api/v1/policy-consents") { consentRoutes() } }
        }
    }

    @Test
    fun `record consent happy path returns 200`() =
        testApplication {
            coEvery { repo.recordConsent(any(), any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/policy-consents/consent") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"policyId":"pol-1"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "pol-1")
        }

    @Test
    fun `record consent without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/policy-consents/consent") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"policyId":"pol-1"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `record consent invalid body returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/policy-consents/consent") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `list user consents happy path returns 200`() =
        testApplication {
            coEvery { repo.getUserConsents(any()) } returns listOf(sample())
            setup()
            val res =
                client.get("/api/v1/policy-consents") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "uc-1")
        }

    @Test
    fun `list user consents without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/policy-consents")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `check consent happy path returns 200`() =
        testApplication {
            coEvery { repo.hasUserConsented(any(), any()) } returns true
            setup()
            val res =
                client.get("/api/v1/policy-consents/PRIVACY_POLICY") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "true")
        }

    @Test
    fun `check consent without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/policy-consents/PRIVACY_POLICY")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `check consent invalid policy type returns 400`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/policy-consents/NOPE") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }
}
