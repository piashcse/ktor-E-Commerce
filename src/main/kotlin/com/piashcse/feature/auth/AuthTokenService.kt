package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.database.entities.ApiKeyDAO
import com.piashcse.database.entities.ApiKeyTable
import com.piashcse.database.entities.RefreshTokenDAO
import com.piashcse.database.entities.RefreshTokenTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.model.request.TokenPair
import com.piashcse.utils.common.AppClock
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.security.MessageDigest
import java.util.UUID

/** Access/refresh token issuance, storage and revocation. */
class AuthTokenService {
    suspend fun generateTokenPair(
        userId: String,
        email: String,
        userType: String,
    ): TokenPair =
        query {
            val tokenVersion = UserDAO.findById(userId)?.tokenVersion ?: 0
            newTokenPair(userId, email, userType, tokenVersion)
        }

    fun newTokenPair(
        userId: String,
        email: String,
        userType: String,
        tokenVersion: Int,
    ): TokenPair {
        val accessToken = JwtConfig.tokenProvider(JwtTokenRequest(userId, email, userType), tokenVersion)
        return TokenPair(accessToken = accessToken, refreshToken = UUID.randomUUID().toString(), expiresIn = 900)
    }

    fun hashRefreshToken(token: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    /** OTPs are bearer secrets; only a keyed digest is persisted. */
    fun hashOtp(otp: String): String =
        com.piashcse.utils.common.hmacSha256Hex(
            com.piashcse.config.DotEnvConfig.jwtSecret,
            "otp:$otp",
        )

    suspend fun storeRefreshToken(
        userId: String,
        refreshToken: String,
    ) = query {
        storeRefreshTokenTx(userId, refreshToken)
    }

    fun storeRefreshTokenTx(
        userId: String,
        refreshToken: String,
    ) {
        val tokenHash = hashRefreshToken(refreshToken)
        val expiresAt = AppClock.now().plusSeconds(AppConstants.Authentication.REFRESH_TOKEN_EXPIRY_SECONDS)
        RefreshTokenDAO.new {
            this.userId = userId.entityID(UserTable)
            this.tokenHash = tokenHash
            this.expiresAt = expiresAt
        }
    }

    suspend fun getRefreshTokenByHash(tokenHash: String): RefreshTokenDAO? =
        query {
            RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.singleOrNull()
        }

    suspend fun revokeRefreshToken(tokenHash: String): Boolean =
        query {
            RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.singleOrNull()
                ?.let {
                    it.revokedAt = AppClock.now()
                    true
                } ?: false
        }

    suspend fun revokeAllUserTokens(userId: String): Boolean =
        query {
            revokeAllUserTokensTx(userId)
            true
        }

    fun revokeAllUserTokensTx(userId: String) {
        RefreshTokenDAO.find { RefreshTokenTable.userId eq userId.entityID(UserTable) }
            .forEach { it.revokedAt = AppClock.now() }
        // Role changes, password resets and deactivation must invalidate API keys
        // as well; otherwise an old key keeps the previous role's access.
        ApiKeyDAO.find {
            (ApiKeyTable.userId eq userId.entityID(UserTable)) and (ApiKeyTable.isActive eq true)
        }.forEach { it.isActive = false }
    }
}
