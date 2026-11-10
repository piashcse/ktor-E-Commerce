package com.piashcse.utils

import com.piashcse.utils.validator.PasswordPolicy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordPolicyTest {
    @Test
    fun `accepts strong passwords`() {
        listOf("Password123!", "A1b2C3d4$", "Str0ng-Pass").forEach {
            assertTrue(PasswordPolicy.isStrong(it), "expected strong: $it")
        }
    }

    @Test
    fun `rejects weak passwords`() {
        listOf(
            "short1!",
            "alllowercase1!",
            "ALLUPPERCASE1!",
            "NoDigits!!",
            "NoSpecial123",
            "Password123!".repeat(6),
        ).forEach {
            assertFalse(PasswordPolicy.isStrong(it), "expected weak: $it")
        }
    }
}
