package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.AppConstants.Authentication.JWT_EXPIRY_SECONDS
import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.mapper.toAuthUser
import com.piashcse.model.domain.AuthUser
import com.piashcse.model.domain.LoginAttemptInfo
import com.piashcse.model.domain.OtpInfo
import com.piashcse.model.domain.StoredRefreshToken
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.model.request.TokenPair
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/**
 * Persistence implementation for the auth feature.
 *
 * Owns row reads/writes, DAO to DTO projection, and token infrastructure
 * (JWT generation, refresh-token hashing). Business rules, authorization,
 * lockout/expiry policy, token rotation, and event publishing live in
 * [UserAuthenticationService].
 */
class AuthRepositoryImpl : AuthRepository {

    // ── Token infrastructure ─────────────────────────────────────────────

    override fun generateTokenPair(userId: String, email: String, userType: String): TokenPair {
        val accessToken = JwtConfig.tokenProvider(JwtTokenRequest(userId, email, userType))
        return TokenPair(accessToken = accessToken, refreshToken = UUID.randomUUID().toString(), expiresIn = JWT_EXPIRY_SECONDS)
    }

    override fun hashRefreshToken(token: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    override suspend fun storeRefreshToken(userId: String, refreshToken: String) {
        val tokenHash = hashRefreshToken(refreshToken)
        val expiresAt = Instant.now().plusSeconds(AppConstants.Authentication.REFRESH_TOKEN_EXPIRY_SECONDS)
        query {
            RefreshTokenDAO.new {
                this.userId = userId.entityID(UserTable)
                this.tokenHash = tokenHash
                this.expiresAt = expiresAt
            }
        }
    }

    override suspend fun getRefreshTokenByHash(tokenHash: String): StoredRefreshToken? = query {
        RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.singleOrNull()?.let {
            StoredRefreshToken(it.userId.value, it.tokenHash, it.expiresAt, it.revokedAt)
        }
    }

    override suspend fun revokeRefreshToken(tokenHash: String): Boolean = query {
        RefreshTokenDAO.find { RefreshTokenTable.tokenHash eq tokenHash }.singleOrNull()
            ?.let { it.revokedAt = Instant.now(); true } ?: false
    }

    override suspend fun revokeAllUserTokens(userId: String): Boolean = query {
        RefreshTokenDAO.find { RefreshTokenTable.userId eq userId.entityID(UserTable) }
            .forEach { it.revokedAt = Instant.now() }
        true
    }

    // ── Login attempt tracking ───────────────────────────────────────────

    private fun loginAttemptPredicate(email: String, userType: UserType) =
        (LoginAttemptTable.email eq email) and (LoginAttemptTable.userType eq userType)

    override suspend fun recordFailedAttempt(email: String, userType: UserType, ipAddress: String?): Int = query {
        val existing = LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()
        if (existing != null) {
            existing.attemptCount++
            existing.ipAddress = ipAddress
            existing.attemptCount
        } else {
            LoginAttemptDAO.new {
                this.email = email
                this.userType = userType
                this.ipAddress = ipAddress
                this.attemptCount = 1
            }
            1
        }
    }

    override suspend fun resetLoginAttempts(email: String, userType: UserType) = query {
        LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()?.apply {
            attemptCount = 0; lockedUntil = null; ipAddress = null
        }
        Unit
    }

    override suspend fun getLoginAttempt(email: String, userType: UserType): LoginAttemptInfo? = query {
        LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()?.let {
            LoginAttemptInfo(it.email, it.userType, it.attemptCount, it.lockedUntil)
        }
    }

    override suspend fun lockAccount(email: String, userType: UserType, lockDurationMinutes: Long): Boolean = query {
        LoginAttemptDAO.find { loginAttemptPredicate(email, userType) }.singleOrNull()
            ?.apply { lockedUntil = Instant.now().plusSeconds(lockDurationMinutes * 60) } != null
    }

    // ── User lookups ─────────────────────────────────────────────────────

    override suspend fun findUserByEmailAndType(email: String, userTypeEnum: UserType): AuthUser? = query {
        UserDAO.find { UserTable.email eq email and (UserTable.userType eq userTypeEnum) }.firstOrNull()?.toAuthUser()
    }

    override suspend fun findUserById(userId: String): AuthUser? = query {
        UserDAO.findById(userId)?.toAuthUser()
    }

    override suspend fun findResetUserByEmail(email: String, userTypeStr: String): AuthUser {
        val entities = UserDAO.find { UserTable.email eq email }.toList()
        if (entities.isEmpty()) email.throwNotFound("User")
        val type = UserType.fromString(userTypeStr)
            ?: throw NotFoundException(Message.Auth.userNotFoundForRole(userTypeStr))
        return entities.find { it.userType == type }?.toAuthUser()
            ?: throw NotFoundException(Message.Auth.userNotFoundForRole(userTypeStr))
    }

    // ── Registration persistence ─────────────────────────────────────────

    override suspend fun getRegistrationOtp(userId: String): String? = query {
        UserDAO.findById(userId)?.otpCode
    }

    override suspend fun getRegistrationOtpExpiry(userId: String): LocalDateTime? = query {
        UserDAO.findById(userId)?.otpExpiry
    }

    override suspend fun createUserWithProfile(
        email: String,
        passwordHash: String,
        userType: UserType,
        otp: String,
        otpExpiry: LocalDateTime,
    ): String = query {
        val inserted = UserDAO.new {
            this.email = email
            this.otpCode = otp
            this.otpExpiry = otpExpiry
            this.password = passwordHash
            this.userType = userType
        }
        UserProfileDAO.new { userId = inserted.id }
        if (userType == UserType.SELLER) {
            SellerDAO.new { userId = inserted.id; status = ShopStatus.PENDING }
        }
        inserted.id.value
    }

    override suspend fun resendRegistrationOtp(userId: String, otp: String, otpExpiry: LocalDateTime) {
        query {
            UserDAO.findById(userId)?.apply {
                this.otpCode = otp
                this.otpExpiry = otpExpiry
            }
        }
    }

    override suspend fun markUserVerified(userId: String) {
        query {
            UserDAO.findById(userId)?.apply {
                isVerified = true
                otpCode = null
                otpExpiry = null
            }
        }
    }

    override suspend fun invalidateOtp(userId: String) {
        query {
            UserDAO.findById(userId)?.apply {
                otpCode = null
                otpExpiry = null
            }
        }
    }

    // ── OTP attempt tracking ─────────────────────────────────────────────

    override suspend fun getOtpAttempt(userId: String): Int = query {
        OtpAttemptDAO.find { OtpAttemptTable.userId eq userId.entityID(UserTable) }
            .singleOrNull()?.attemptCount ?: 0
    }

    override suspend fun isOtpLocked(userId: String): Boolean = query {
        OtpAttemptDAO.find { OtpAttemptTable.userId eq userId.entityID(UserTable) }
            .singleOrNull()?.isLocked == true
    }

    override suspend fun recordFailedOtpAttempt(userId: String): Int = query {
        val existing = OtpAttemptDAO.find { OtpAttemptTable.userId eq userId.entityID(UserTable) }
            .singleOrNull()
        if (existing != null) {
            existing.attemptCount++
            existing.attemptCount
        } else {
            OtpAttemptDAO.new {
                this.userId = userId.entityID(UserTable)
                this.attemptCount = 1
            }
            1
        }
    }

    override suspend fun resetOtpAttempts(userId: String) {
        query {
            OtpAttemptDAO.find { OtpAttemptTable.userId eq userId.entityID(UserTable) }
                .singleOrNull()?.delete()
        }
    }

    override suspend fun lockOtpAttempts(userId: String) {
        query {
            OtpAttemptDAO.find { OtpAttemptTable.userId eq userId.entityID(UserTable) }
                .singleOrNull()?.apply {
                    lockedUntil = Instant.now().plusSeconds(AppConstants.Authentication.OTP_LOCKOUT_MINUTES * 60)
                }
        }
    }

    // ── Password / reset persistence ─────────────────────────────────────

    override suspend fun updatePasswordHash(userId: String, newPasswordHash: String) {
        query {
            UserDAO.findById(userId)?.apply { password = newPasswordHash }
        }
    }

    override suspend fun setResetOtp(userId: String, otp: String, otpExpiry: LocalDateTime) {
        query {
            UserDAO.findById(userId)?.apply {
                resetOtpCode = otp
                resetOtpExpiry = otpExpiry
            }
        }
    }

    override suspend fun getResetOtp(userId: String): OtpInfo? = query {
        UserDAO.findById(userId)?.let { OtpInfo(it.resetOtpCode, it.resetOtpExpiry) }
    }

    override suspend fun clearResetOtp(userId: String) {
        query {
            UserDAO.findById(userId)?.apply {
                resetOtpCode = null
                resetOtpExpiry = null
            }
        }
    }

    // ── Blacklist persistence ────────────────────────────────────────────

    override suspend fun insertBlacklistedToken(token: String) {
        query {
            if (BlacklistedTokenDAO.find { BlacklistedTokenTable.token eq token }.firstOrNull() == null) {
                BlacklistedTokenDAO.new { this.token = token; this.blacklistedAt = Instant.now() }
            }
        }
    }

    // ── Admin persistence (no authorization) ─────────────────────────────

    override suspend fun updateUserType(userId: String, newUserType: UserType) {
        query {
            UserDAO.findById(userId)?.apply { userType = newUserType }
        }
    }

    override suspend fun createSellerProfileIfMissing(userId: String) {
        query {
            if (SellerDAO.find { SellerTable.userId eq userId.entityID(UserTable) }.firstOrNull() == null) {
                SellerDAO.new {
                    this.userId = userId.entityID(UserTable)
                    status = ShopStatus.PENDING
                }
            }
        }
    }

    override suspend fun setUserActive(userId: String, active: Boolean) {
        query {
            UserDAO.findById(userId)?.apply { isActive = active }
        }
    }
}
