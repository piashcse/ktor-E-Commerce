package com.piashcse.feature.auth

import com.piashcse.database.entities.*
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.model.response.TokenPair
import com.piashcse.utils.UnauthorizedException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import java.security.MessageDigest
import java.time.LocalDateTime

/**
 * Service for managing JWT access/refresh tokens and token revocation.
 */
class TokenService {

    /**
     * Creates a new token pair (access + refresh) for the user.
     */
    suspend fun createTokenPair(
        userId: String,
        email: String,
        userType: String,
        ipAddress: String? = null,
        userAgent: String? = null
    ): TokenPair {
        val jwtRequest = JwtTokenRequest(userId, email, userType)
        val accessToken = JwtConfig.generateAccessToken(jwtRequest)
        val refreshToken = JwtConfig.generateRefreshToken(jwtRequest)

        // Save refresh token to database
        saveRefreshToken(userId, refreshToken, ipAddress, userAgent)

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = JwtConfig.accessTokenValidityMs / 1000
        )
    }

    /**
     * Saves a refresh token to the database.
     */
    private suspend fun saveRefreshToken(
        userId: String,
        token: String,
        ipAddress: String?,
        userAgent: String?
    ) = query {
        val expiresAt = LocalDateTime.now().plusSeconds(JwtConfig.refreshTokenValidityMs / 1000)

        RefreshTokenDAO.new {
            this.userId = EntityID(userId, UserTable)
            this.token = token
            this.expiresAt = expiresAt
            this.isRevoked = false
            this.lastUsedAt = null
            this.ipAddress = ipAddress
            this.userAgent = userAgent
        }
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * Implements rotating refresh tokens: old refresh token is revoked, new pair issued.
     */
    suspend fun refreshAccessToken(
        refreshToken: String,
        ipAddress: String? = null,
        userAgent: String? = null
    ): TokenPair = query {
        // Find the refresh token in database
        val storedToken = RefreshTokenDAO.find {
            RefreshTokenTable.token eq refreshToken
        }.singleOrNull()

        if (storedToken == null) {
            throw UnauthorizedException("Invalid refresh token")
        }

        // Check if revoked
        if (storedToken.isRevoked) {
            throw UnauthorizedException("Refresh token has been revoked")
        }

        // Check if expired
        if (storedToken.expiresAt < LocalDateTime.now()) {
            storedToken.delete()
            throw UnauthorizedException("Refresh token has expired")
        }

        // Get user info
        val user = UserDAO.findById(storedToken.userId.value)
            ?: throw storedToken.userId.value.notFoundException()

        // Verify user is still active
        if (!user.isActive) {
            throw UnauthorizedException("Account has been deactivated")
        }

        // Revoke the old refresh token (rotation)
        storedToken.isRevoked = true
        storedToken.lastUsedAt = LocalDateTime.now()

        // Generate new token pair
        val jwtRequest = JwtTokenRequest(user.id.value, user.email, user.userType.name)
        val newAccessToken = JwtConfig.generateAccessToken(jwtRequest)
        val newRefreshToken = JwtConfig.generateRefreshToken(jwtRequest)

        // Save new refresh token
        val expiresAt = LocalDateTime.now().plusSeconds(JwtConfig.refreshTokenValidityMs / 1000)
        RefreshTokenDAO.new {
            this.userId = user.id
            this.token = newRefreshToken
            this.expiresAt = expiresAt
            this.isRevoked = false
            this.lastUsedAt = null
            this.ipAddress = ipAddress
            this.userAgent = userAgent
        }

        TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = JwtConfig.accessTokenValidityMs / 1000
        )
    }

    /**
     * Revokes a single refresh token (logout).
     */
    suspend fun revokeRefreshToken(refreshToken: String) = query {
        val storedToken = RefreshTokenDAO.find {
            RefreshTokenTable.token eq refreshToken
        }.singleOrNull()

        storedToken?.let {
            it.isRevoked = true
            it.lastUsedAt = LocalDateTime.now()
        }
    }

    /**
     * Revokes all refresh tokens for a user (logout all devices).
     */
    suspend fun revokeAllUserTokens(userId: String) = query {
        RefreshTokenDAO.find {
            RefreshTokenTable.userId eq userId and (RefreshTokenTable.isRevoked eq false)
        }.forEach {
            it.isRevoked = true
            it.lastUsedAt = LocalDateTime.now()
        }
    }

    /**
     * Revokes all expired refresh tokens (cleanup).
     * Call this periodically.
     */
    suspend fun cleanupExpiredTokens() = query {
        RefreshTokenDAO.find {
            RefreshTokenTable.expiresAt less LocalDateTime.now()
        }.forEach { it.delete() }
    }

    /**
     * Checks if an access token hash is in the blacklist.
     */
    suspend fun isTokenRevoked(tokenHash: String): Boolean = query {
        RevokedTokenDAO.find {
            RevokedTokenTable.tokenHash eq tokenHash
        }.singleOrNull() != null
    }

    /**
     * Adds an access token to the blacklist.
     */
    suspend fun revokeAccessToken(userId: String, tokenHash: String, expiresAt: LocalDateTime) = query {
        RevokedTokenDAO.new {
            this.userId = EntityID(userId, UserTable)
            this.tokenHash = tokenHash
            this.expiresAt = expiresAt
        }
    }

    /**
     * Cleans up expired revoked tokens from blacklist.
     */
    suspend fun cleanupRevokedTokens() = query {
        RevokedTokenDAO.find {
            RevokedTokenTable.expiresAt less LocalDateTime.now()
        }.forEach { it.delete() }
    }

    companion object {
        /**
         * Hash a token for secure storage (SHA-256)
         */
        fun hashToken(token: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(token.toByteArray())
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}
