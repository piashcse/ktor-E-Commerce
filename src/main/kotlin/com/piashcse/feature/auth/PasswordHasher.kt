package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants

/** Shared BCrypt hashing/verification for register, reset and password change. */
internal object PasswordHasher {
    fun hash(password: String): String = BCrypt.withDefaults().hashToString(AppConstants.BCRYPT_COST, password.toCharArray())

    fun verify(
        raw: String,
        hash: String,
    ): Boolean = BCrypt.verifyer().verify(raw.toCharArray(), hash).verified
}
