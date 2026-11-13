package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.auth.AuthRepository
import com.piashcse.feature.auth.MfaService
import com.piashcse.feature.auth.MfaSetupResponse
import com.piashcse.feature.auth.UserAuthenticationService
import com.piashcse.feature.auth.authRoutes
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

class MfaRoutesTest {
    private val authRepo: AuthRepository = mockk(relaxed = true)
    private val userAuthService: UserAuthenticationService = mockk(relaxed = true)
    private val mfaService: MfaService = mockk()

    private fun ApplicationTestBuilder.setup() {
        coEvery { authRepo.verifyUserPassword("user-1", any()) } returns true
        application {
            installTestInfra(
                module {
                    single { authRepo }
                    single { userAuthService }
                    single { mfaService }
                },
            )
            routing { route("/api/v1/auth") { authRoutes() } }
        }
    }

    @Test
    fun `authenticated user starts MFA setup`() =
        testApplication {
            coEvery { mfaService.setup("user-1") } returns
                MfaSetupResponse("SECRET", "otpauth://totp/example")
            setup()
            val res =
                client.post("/api/v1/auth/mfa/setup") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"password":"Password1!"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "otpauth")
        }

    @Test
    fun `authenticated user enables MFA`() =
        testApplication {
            coEvery { mfaService.enable("user-1", "123456") } returns true
            setup()
            val res =
                client.post("/api/v1/auth/mfa/enable") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"code":"123456","password":"Password1!"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "MFA enabled")
        }

    @Test
    fun `anonymous cannot start MFA setup`() =
        testApplication {
            setup()
            assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/auth/mfa/setup").status)
        }
}
