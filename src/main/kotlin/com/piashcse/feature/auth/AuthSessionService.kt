package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.model.request.*
import com.piashcse.service.CacheService
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.UnauthorizedException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq
import java.time.Instant
import java.util.*

/** Refresh-token rotation, logout and token blacklisting. */
class AuthSessionService {
    private val tokens = AuthTokenService()

    suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair =
        query {
            val tokenHash = tokens.hashRefreshToken(request.refreshToken)
            val storedToken =
                RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.forUpdate().singleOrNull()
                    ?: throw UnauthorizedException(Message.Auth.INVALID_REFRESH_TOKEN)

            // A normally expired token is stale, not evidence of theft. Only a
            // revoked token indicates replay/reuse and should kill the family.
            if (storedToken.isExpired) {
                throw UnauthorizedException(Message.Auth.INVALID_REFRESH_TOKEN)
            }
            if (storedToken.isRevoked) {
                UserDAO.findById(storedToken.userId.value)?.tokenVersion += 1
                runCatching { tokens.revokeAllUserTokensTx(storedToken.userId.value) }
                throw UnauthorizedException(Message.Auth.TOKEN_EXPIRED)
            }

            val user = UserDAO.findById(storedToken.userId.value) ?: throw NotFoundException(Message.Errors.NOT_FOUND)
            if (!user.isActive) throw ValidationException(Message.Auth.ACCOUNT_DEACTIVATED)
            if (!user.isVerified) throw ValidationException(Message.Auth.ACCOUNT_NOT_VERIFIED)

            // Rotate old + persist replacement in the same transaction so a
            // concurrent password reset cannot leave a freshly minted token live.
            storedToken.revokedAt = Instant.now()
            val newTokenPair = tokens.newTokenPair(user.id.value, user.email, user.userType.name, user.tokenVersion)
            tokens.storeRefreshTokenTx(user.id.value, newTokenPair.refreshToken)
            newTokenPair
        }

    suspend fun logout(
        userId: String,
        refreshToken: String?,
    ): Boolean {
        if (!refreshToken.isNullOrBlank()) {
            tokens.revokeRefreshToken(tokens.hashRefreshToken(refreshToken))
        } else {
            // Logging out a browser session must not deactivate machine API
            // keys; those are revoked by password/role changes or explicitly.
            query {
                RefreshTokenDAO.find { RefreshTokenTable.userId eq userId.entityID(UserTable) }
                    .forEach { it.revokedAt = Instant.now() }
            }
        }
        return true
    }

    suspend fun blacklistToken(token: String): Boolean {
        val result =
            query {
                if (BlacklistedTokenDAO.find { BlacklistedTokenTable.token eq token }.firstOrNull() == null) {
                    BlacklistedTokenDAO.new {
                        this.token = token
                        this.blacklistedAt = Instant.now()
                    }
                }
                true
            }
        CacheService.cache.set("blacklisted_token:$token", true, AppConstants.Authentication.JWT_EXPIRY_SECONDS)
        return result
    }
}
