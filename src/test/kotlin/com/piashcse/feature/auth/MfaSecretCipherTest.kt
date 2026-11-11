package com.piashcse.feature.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MfaSecretCipherTest {
    private val cipher = MfaSecretCipher("unit-test-key-material")

    @Test
    fun `encrypts and decrypts TOTP secrets`() {
        val encrypted = cipher.encrypt("JBSWY3DPEHPK3PXP")
        assertTrue(encrypted.startsWith("enc:v1:"))
        assertTrue(!encrypted.contains("JBSWY3DP"))
        assertEquals("JBSWY3DPEHPK3PXP", cipher.decrypt(encrypted))
    }

    @Test
    fun `supports legacy plaintext secrets`() {
        assertEquals("JBSWY3DPEHPK3PXP", cipher.decrypt("JBSWY3DPEHPK3PXP"))
    }
}
