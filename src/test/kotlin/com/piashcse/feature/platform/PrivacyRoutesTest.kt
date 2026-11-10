package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.privacy.PrivacyService
import com.piashcse.feature.privacy.privacyRoutes
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

class PrivacyRoutesTest {
    private val privacyService: PrivacyService = mockk()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { privacyService } })
            routing { route("/api/v1/privacy") { privacyRoutes() } }
        }
    }

    @Test
    fun `erase requires the account password`() =
        testApplication {
            coEvery { privacyService.erase("user-1", "secret123") } returns true
            setup()
            val res =
                client.post("/api/v1/privacy/erase") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"password":"secret123"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "erased")
        }

    @Test
    fun `erase with wrong password returns 401`() =
        testApplication {
            coEvery { privacyService.erase("user-1", "wrong") } throws
                com.piashcse.utils.validator.UnauthorizedException()
            setup()
            val res =
                client.post("/api/v1/privacy/erase") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"password":"wrong"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `anonymous cannot erase`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/privacy/erase") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"password":"secret123"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
