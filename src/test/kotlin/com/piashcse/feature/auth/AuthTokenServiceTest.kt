package com.piashcse.feature.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AuthTokenServiceTest {
    private val tokens = AuthTokenService()

    @Test
    fun `refresh token hash is stable lowercase hex`() {
        val first = tokens.hashRefreshToken("refresh-token-1")
        val second = tokens.hashRefreshToken("refresh-token-1")
        assertEquals(first, second)
        assertEquals(64, first.length)
        assert(first.all { it in "0123456789abcdef" })
        assertNotEquals(first, tokens.hashRefreshToken("refresh-token-2"))
    }
}
