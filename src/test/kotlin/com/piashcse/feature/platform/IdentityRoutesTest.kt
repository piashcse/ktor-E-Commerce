package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.database.entities.LoginResponse
import com.piashcse.feature.auth.AuthRepository
import com.piashcse.feature.auth.PasskeyCredentialResponse
import com.piashcse.feature.auth.PasskeyLoginOptionsResponse
import com.piashcse.feature.auth.PasskeyRegisterOptionsResponse
import com.piashcse.feature.auth.PasskeyService
import com.piashcse.feature.auth.SocialAuthService
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

class IdentityRoutesTest {
    private val authRepo: AuthRepository = mockk(relaxed = true)
    private val userAuth: UserAuthenticationService = mockk(relaxed = true)
    private val social: SocialAuthService = mockk()
    private val passkeys: PasskeyService = mockk()

    private fun loginResponse() = LoginResponse(null, "access-1", "refresh-1", 900)

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single { authRepo }
                    single { userAuth }
                    single { social }
                    single { passkeys }
                },
            )
            routing { route("/api/v1/auth") { authRoutes() } }
        }
    }

    @Test
    fun `google login returns tokens`() =
        testApplication {
            coEvery { social.googleLogin("google-id-token", null) } returns loginResponse()
            setup()
            val res =
                client.post("/api/v1/auth/social/google") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"idToken":"google-id-token"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "access-1")
        }

    @Test
    fun `passkey login options and assertion`() =
        testApplication {
            coEvery { passkeys.loginOptions(any()) } returns PasskeyLoginOptionsResponse("challenge", "localhost")
            coEvery { passkeys.login(any()) } returns loginResponse()
            setup()
            val options =
                client.post("/api/v1/auth/passkeys/login/options") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"test@example.com"}""")
                }
            assertEquals(HttpStatusCode.OK, options.status)
            assertContains(options.bodyAsText(), "challenge")
            val login =
                client.post("/api/v1/auth/passkeys/login") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {"email":"test@example.com","credentialId":"cred","clientDataJson":"x",
                         "authenticatorData":"y","signature":"z"}
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.OK, login.status)
        }

    @Test
    fun `authenticated user registers and lists passkeys`() =
        testApplication {
            coEvery { passkeys.registerOptions("user-1") } returns
                PasskeyRegisterOptionsResponse("challenge", "localhost", "Shop", "user", "test@example.com", "test@example.com")
            coEvery { passkeys.register("user-1", any()) } returns
                PasskeyCredentialResponse("pk-1", "cred-1", "Laptop", null, 0, null, null)
            coEvery { passkeys.list("user-1") } returns
                listOf(PasskeyCredentialResponse("pk-1", "cred-1", "Laptop", null, 0, null, null))
            coEvery { passkeys.delete("user-1", "pk-1") } returns true
            setup()
            val options =
                client.post("/api/v1/auth/passkeys/register/options") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, options.status)
            val registered =
                client.post("/api/v1/auth/passkeys/register") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"credentialId":"cred-1","clientDataJson":"x","publicKey":"y","name":"Laptop"}""",
                    )
                }
            assertEquals(HttpStatusCode.OK, registered.status)
            val listed =
                client.get("/api/v1/auth/passkeys") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, listed.status)
            assertContains(listed.bodyAsText(), "cred-1")
            val deleted =
                client.delete("/api/v1/auth/passkeys/pk-1") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, deleted.status)
        }
}
