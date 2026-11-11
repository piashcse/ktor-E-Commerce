package com.piashcse.feature.auth

import com.piashcse.utils.validator.ValidationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GoogleTokenVerifierTest {
    private val clientId = "client-123"
    private val now = 1_700_000_000_000L

    private class FakeTransport(
        private val status: Int,
        private val body: String,
    ) : SocialHttpTransport {
        override suspend fun get(
            url: String,
            headers: Map<String, String>,
        ): SocialHttpResponse = SocialHttpResponse(status, body)
    }

    private fun verifier(
        status: Int = 200,
        body: String = validBody(),
    ) = GoogleTokenVerifier(clientId, FakeTransport(status, body), clock = { now })

    private fun validBody(
        aud: String = "client-123",
        verified: String = "true",
        exp: String = "1700003600",
    ) = """{"aud":"$aud","email":"User@Example.com","email_verified":"$verified","exp":"$exp","sub":"google-1","name":"User"}"""

    @Test
    fun `verifies a valid Google token`() =
        runBlocking {
            val identity = verifier().verify("id-token")
            assertEquals("user@example.com", identity.email)
            assertEquals("google-1", identity.subject)
        }

    @Test
    fun `rejects audience mismatch`(): Unit =
        runBlocking {
            assertFailsWith<ValidationException> {
                verifier(body = validBody(aud = "other")).verify("id-token")
            }
        }

    @Test
    fun `rejects unverified and expired tokens`(): Unit =
        runBlocking {
            assertFailsWith<ValidationException> {
                verifier(body = validBody(verified = "false")).verify("id-token")
            }
            assertFailsWith<ValidationException> {
                verifier(body = validBody(exp = "1")).verify("id-token")
            }
        }

    @Test
    fun `rejects provider errors and unconfigured clients`(): Unit =
        runBlocking {
            assertFailsWith<ValidationException> { verifier(status = 401).verify("id-token") }
            assertFailsWith<ValidationException> {
                GoogleTokenVerifier("", FakeTransport(200, validBody())).verify("id-token")
            }
            assertFailsWith<ValidationException> { verifier().verify("") }
        }
}
