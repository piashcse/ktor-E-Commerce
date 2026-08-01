package com.piashcse.model.domain

import java.time.Instant

/**
 * Domain representation of a stored refresh token, exposing only the fields
 * the auth layer needs instead of the underlying DAO.
 */
data class StoredRefreshToken(
    val userId: String,
    val tokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
) {
    val isValid: Boolean get() = !isExpired && !isRevoked
    val isExpired: Boolean get() = expiresAt.isBefore(Instant.now())
    val isRevoked: Boolean get() = revokedAt != null
}
