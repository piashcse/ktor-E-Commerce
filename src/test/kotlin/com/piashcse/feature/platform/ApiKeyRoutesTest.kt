package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.apikey.ApiKeyCreatedResponse
import com.piashcse.feature.apikey.ApiKeyResponse
import com.piashcse.feature.apikey.ApiKeyService
import com.piashcse.feature.apikey.apiKeyRoutes
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

class ApiKeyRoutesTest {
    private val service: ApiKeyService = mockk()

    private fun created() =
        ApiKeyCreatedResponse(
            id = "key-1",
            name = "CI",
            prefix = "ak_live",
            scopes = listOf("API_KEY_MANAGE"),
            key = "ak_live_secret",
            createdAt = "2024-01-01T00:00:00Z",
        )

    private fun listed() =
        ApiKeyResponse(
            id = "key-1",
            name = "CI",
            prefix = "ak_live",
            scopes = listOf("API_KEY_MANAGE"),
            isActive = true,
            lastUsedAt = null,
            expiresAt = null,
            createdAt = "2024-01-01T00:00:00Z",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing { route("/api/v1/api-keys") { apiKeyRoutes() } }
        }
    }

    @Test
    fun `create returns the plaintext key once`() =
        testApplication {
            coEvery { service.create("user-1", any()) } returns created()
            setup()
            val res =
                client.post("/api/v1/api-keys") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"CI"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "ak_live_secret")
        }

    @Test
    fun `list returns keys`() =
        testApplication {
            coEvery { service.list("user-1") } returns listOf(listed())
            setup()
            val res =
                client.get("/api/v1/api-keys") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "key-1")
        }

    @Test
    fun `revoke unknown key returns 404`() =
        testApplication {
            coEvery { service.revoke("user-1", "missing") } returns false
            setup()
            val res =
                client.delete("/api/v1/api-keys/missing") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.NotFound, res.status)
        }

    @Test
    fun `anonymous is unauthorized`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/api-keys")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
