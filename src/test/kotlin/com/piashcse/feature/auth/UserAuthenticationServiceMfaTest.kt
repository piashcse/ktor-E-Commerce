package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.UserType
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.TokenPair
import com.piashcse.utils.validator.InvalidMfaCodeException
import com.piashcse.utils.validator.MfaRequiredException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserAuthenticationServiceMfaTest {
    private val secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    private val mfaService = MfaService()
    private val authRepo: AuthRepository = mockk(relaxed = true)
    private val service = UserAuthenticationService(authRepo, mfaService)

    private fun userWithMfa() =
        mockk<UserDAO>(relaxed = true) {
            every { id } returns EntityID("user-1", UserTable)
            every { email } returns "test@example.com"
            every { password } returns BCrypt.withDefaults().hashToString(4, "Password1!".toCharArray())
            every { userType } returns UserType.CUSTOMER
            every { isVerified } returns true
            every { isActive } returns true
            every { mfaEnabled } returns true
            every { mfaSecret } returns secret
            every { createdAt } returns LocalDateTime.of(2024, 1, 1, 0, 0)
            every { updatedAt } returns null
        }

    private fun request(code: String?) = LoginRequest("test@example.com", "Password1!", "customer", code)

    @Test
    fun `login without a code requires MFA`(): Unit =
        runBlocking {
            coEvery { authRepo.findUserByEmailAndType(any(), any()) } returns userWithMfa()
            coEvery { authRepo.recordFailedAttempt(any(), any(), any()) } returns 1

            assertFailsWith<MfaRequiredException> { service.login(request(null)) }
        }

    @Test
    fun `login with a wrong code is rejected`(): Unit =
        runBlocking {
            coEvery { authRepo.findUserByEmailAndType(any(), any()) } returns userWithMfa()
            coEvery { authRepo.recordFailedAttempt(any(), any(), any()) } returns 1

            assertFailsWith<InvalidMfaCodeException> { service.login(request("000000")) }
        }

    @Test
    fun `login with a valid code returns tokens`(): Unit =
        runBlocking {
            val code = mfaService.generateCode(secret, System.currentTimeMillis() / 1000 / 30)
            coEvery { authRepo.findUserByEmailAndType(any(), any()) } returns userWithMfa()
            coEvery { authRepo.generateTokenPair(any(), any(), any()) } returns
                TokenPair("access-1", "refresh-1", "Bearer", 900)

            val response = service.login(request(code))

            assertEquals("access-1", response.accessToken)
            coVerify(exactly = 1) { authRepo.storeRefreshToken("user-1", "refresh-1") }
        }
}
