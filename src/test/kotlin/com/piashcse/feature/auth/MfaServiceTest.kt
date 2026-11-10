package com.piashcse.feature.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MfaServiceTest {
    // RFC 6238 test secret: ASCII "12345678901234567890".
    private val secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    private val service = MfaService()

    @Test
    fun `matches the RFC 6238 TOTP vector`() {
        assertEquals("287082", service.generateCode(secret, 1))
        assertTrue(service.verify(secret, "287082", nowMillis = 59_000))
        assertFalse(service.verify(secret, "287083", nowMillis = 59_000))
    }

    @Test
    fun `accepts the adjacent time step`() {
        val code = service.generateCode(secret, 2)
        assertTrue(service.verify(secret, code, nowMillis = 89_000))
    }

    @Test
    fun `base32 round-trips`() {
        val bytes = "12345678901234567890".toByteArray()
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", Base32.encode(bytes))
        assertEquals("12345678901234567890", String(Base32.decode(Base32.encode(bytes))))
    }
}
