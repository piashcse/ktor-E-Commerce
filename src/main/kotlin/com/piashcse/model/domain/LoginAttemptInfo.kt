package com.piashcse.model.domain

import com.piashcse.constants.UserType
import java.time.Instant

/**
 * Domain representation of a login attempt record used for brute-force
 * protection, decoupled from the persistence layer.
 */
data class LoginAttemptInfo(
    val email: String,
    val userType: UserType,
    val attemptCount: Int,
    val lockedUntil: Instant?,
) {
    val isLocked: Boolean get() = lockedUntil?.isAfter(Instant.now()) == true
}
