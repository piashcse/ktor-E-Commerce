package com.piashcse.utils.validator

/**
 * Shared password strength policy enforced on every password-setting path.
 * Requires 8-64 chars with at least one lowercase, uppercase, digit and special character.
 */
object PasswordPolicy {
    private val pattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$")

    fun isStrong(password: String): Boolean = pattern.matches(password)
}
