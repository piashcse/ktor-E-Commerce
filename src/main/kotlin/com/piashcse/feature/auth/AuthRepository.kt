package com.piashcse.feature.auth

import com.piashcse.constants.OtpPurpose
import com.piashcse.constants.UserType
import com.piashcse.model.domain.AuthUser
import com.piashcse.model.domain.LoginAttemptInfo
import com.piashcse.model.domain.OtpInfo
import com.piashcse.model.domain.StoredRefreshToken
import com.piashcse.model.request.TokenPair
import java.time.LocalDateTime

/**
 * Persistence contract for the auth feature.
 *
 * Deliberately free of business logic: no role checks, no lockout/expiry policy,
 * no token-rotation orchestration, no cache side-effects, no event publishing.
 * Those belong to [UserAuthenticationService].
 */
interface AuthRepository {
    // Registration (persistence + fact reads)
    suspend fun findUserByEmailAndType(email: String, userTypeEnum: UserType): AuthUser?
    suspend fun findUserById(userId: String): AuthUser?
    suspend fun findResetUserByEmail(email: String, userTypeStr: String): AuthUser
    suspend fun getRegistrationOtp(userId: String): String?
    suspend fun getRegistrationOtpExpiry(userId: String): LocalDateTime?
    suspend fun createUserWithProfile(
        email: String,
        passwordHash: String,
        userType: UserType,
        otp: String,
        otpExpiry: LocalDateTime,
    ): String
    suspend fun resendRegistrationOtp(userId: String, otp: String, otpExpiry: LocalDateTime)
    suspend fun markUserVerified(userId: String)
    suspend fun invalidateOtp(userId: String)

    // Token management
    suspend fun storeRefreshToken(userId: String, refreshToken: String)
    suspend fun getRefreshTokenByHash(tokenHash: String): StoredRefreshToken?
    suspend fun revokeRefreshToken(tokenHash: String): Boolean
    suspend fun revokeAllUserTokens(userId: String): Boolean
    fun generateTokenPair(userId: String, email: String, userType: String): TokenPair
    fun hashRefreshToken(token: String): String

    // Login attempt tracking
    suspend fun getLoginAttempt(email: String, userType: UserType): LoginAttemptInfo?
    suspend fun recordFailedAttempt(email: String, userType: UserType, ipAddress: String?): Int
    suspend fun resetLoginAttempts(email: String, userType: UserType)
    suspend fun lockAccount(email: String, userType: UserType, lockDurationMinutes: Long): Boolean

    // OTP attempt tracking
    suspend fun getOtpAttempt(userId: String, purpose: OtpPurpose): Int
    suspend fun isOtpLocked(userId: String, purpose: OtpPurpose): Boolean
    suspend fun recordFailedOtpAttempt(userId: String, purpose: OtpPurpose): Int
    suspend fun resetOtpAttempts(userId: String, purpose: OtpPurpose)
    suspend fun lockOtpAttempts(userId: String, purpose: OtpPurpose)

    // Password / reset persistence
    suspend fun updatePasswordHash(userId: String, newPasswordHash: String)
    suspend fun setResetOtp(userId: String, otp: String, otpExpiry: LocalDateTime)
    suspend fun getResetOtp(userId: String): OtpInfo?
    suspend fun clearResetOtp(userId: String)

    // Blacklist persistence
    suspend fun insertBlacklistedToken(token: String)

    // Admin (persistence only; authorization is owned by the service)
    suspend fun updateUserType(userId: String, newUserType: UserType)
    suspend fun createSellerProfileIfMissing(userId: String)
    suspend fun setUserActive(userId: String, active: Boolean)
}
