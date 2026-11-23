package com.piashcse.feature.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {
    @Test
    fun `hash is salted and verifies the original password`() {
        val first = PasswordHasher.hash("correct horse battery staple")
        val second = PasswordHasher.hash("correct horse battery staple")
        assertNotEquals(first, second)
        assertTrue(PasswordHasher.verify("correct horse battery staple", first))
        assertFalse(PasswordHasher.verify("wrong password", first))
    }
}
