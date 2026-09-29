package com.piashcse.feature.auth

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class AuthRoutesTest {
    private val authRepo: AuthRepository = mockk()
    private val userAuthService = UserAuthenticationService(authRepo)

    private fun mockUser() =
        mockk<UserDAO> {
            every { id } returns EntityID("user-1", UserTable)
            every { email } returns "test@example.com"
            every { userType } returns com.piashcse.constants.UserType.CUSTOMER
            every { isVerified } returns true
            every { isActive } returns true
            every { createdAt } returns LocalDateTime.of(2024, 1, 1, 0, 0)
            every { updatedAt } returns null
        }

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                org.koin.dsl.module {
                    single<AuthRepository> { authRepo }
                    single { userAuthService }
                },
            )
            routing { route("/api/v1/auth") { authRoutes() } }
        }
    }

    @Test
    fun `login rejects invalid email with 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"not-an-email","password":"Password1!","userType":"customer"}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `register happy path returns 201`() =
        testApplication {
            coEvery { authRepo.register(any()) } returns RegistrationResult.Created("user-1", "test@example.com", "OTP sent")
            coEvery { authRepo.getRegistrationOtp("user-1") } returns "123456"
            setup()
            val res =
                client.post("/api/v1/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"test@example.com","password":"Password1!","userType":"customer"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
        }

    @Test
    fun `register weak password returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"test@example.com","password":"weakpass1","userType":"customer"}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `forgot-password returns generic success`() =
        testApplication {
            coEvery { authRepo.findResetUserByEmail(any(), any()) } returns mockUser()
            coEvery { authRepo.forgotPassword(any()) } returns "123456"
            setup()
            val res =
                client.post("/api/v1/auth/forgot-password") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"test@example.com","userType":"customer"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `reset-password locked returns 429`() =
        testApplication {
            coEvery { authRepo.resetPassword(any()) } returns ResetResult.Locked
            setup()
            val res =
                client.post("/api/v1/auth/reset-password") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"test@example.com","userType":"customer","verificationCode":"000000","newPassword":"Password1!"}""")
                }
            assertEquals(HttpStatusCode.TooManyRequests, res.status)
        }

    @Test
    fun `otp-verification missing params returns 400`() =
        testApplication {
            setup()
            val res = client.post("/api/v1/auth/otp-verification")
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `refresh-token happy path returns 200`() =
        testApplication {
            coEvery { authRepo.refreshAccessToken(any()) } returns
                com.piashcse.model.request.TokenPair("access", "refresh", "Bearer", 900)
            setup()
            val res =
                client.post("/api/v1/auth/refresh-token") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"refreshToken":"old-refresh"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "access")
        }

    @Test
    fun `logout without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/auth/logout") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"refreshToken":"r"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `logout with token returns 200`() =
        testApplication {
            coEvery { authRepo.blacklistToken(any()) } returns true
            coEvery { authRepo.logout(any(), any()) } returns true
            setup()
            val res =
                client.post("/api/v1/auth/logout") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"refreshToken":"r"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `change-password weak new password returns 400`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/auth/change-password") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"oldPassword":"Password1!","newPassword":"weak"}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }
}
