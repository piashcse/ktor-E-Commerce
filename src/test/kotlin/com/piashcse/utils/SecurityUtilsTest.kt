package com.piashcse.utils

import com.piashcse.utils.common.generateOTP
import com.piashcse.utils.common.generateToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SecurityUtilsTest {
    @Test
    fun `otp has the requested length and digit range`() {
        repeat(100) {
            val otp = generateOTP(6)
            assertEquals(6, otp.length)
            assertTrue(otp.all { it.isDigit() })
            assertTrue(otp.toInt() in 100_000..999_999)
        }
    }

    @Test
    fun `otp rejects unsupported lengths`() {
        assertFailsWith<IllegalArgumentException> { generateOTP(0) }
        assertFailsWith<IllegalArgumentException> { generateOTP(10) }
    }

    @Test
    fun `tokens are 32 random bytes hex encoded`() {
        val token = generateToken()
        assertEquals(64, token.length)
        assertTrue(token.matches(Regex("[0-9a-f]{64}")))
    }
}
