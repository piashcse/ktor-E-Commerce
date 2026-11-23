package com.piashcse.feature.auth

import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.database.entities.LoginAttemptDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.model.request.*
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult

/** Aggregate auth facade; splitting it would fragment session/OTP invariants. */
interface AuthRepository {
    // Registration
    suspend fun register(registerRequest: RegisterRequest): RegistrationResult

    suspend fun getRegistrationOtp(userId: String): String?

    // Login
    suspend fun findUserByEmailAndType(
        email: String,
        userTypeEnum: UserType,
    ): UserDAO?

    /**
     * Atomically records the last accepted TOTP step for a user. Must run inside a
     * repository transaction; mutating a DAO returned by [findUserByEmailAndType]
     * after that call's transaction has closed throws.
     */
    suspend fun recordMfaStep(
        userId: String,
        step: Long,
    )

    /** Verifies a user's current password for sensitive step-up operations. */
    suspend fun verifyUserPassword(
        userId: String,
        rawPassword: String,
    ): Boolean

    suspend fun findResetUserByEmail(
        email: String,
        userTypeStr: String,
    ): UserDAO

    // Token management
    suspend fun storeRefreshToken(
        userId: String,
        refreshToken: String,
    )

    suspend fun generateTokenPair(
        userId: String,
        email: String,
        userType: String,
    ): TokenPair

    // Login attempt tracking
    suspend fun getLoginAttempt(
        email: String,
        userType: UserType,
        ipAddress: String? = null,
    ): LoginAttemptDAO?

    suspend fun recordFailedAttempt(
        email: String,
        userType: UserType,
        ipAddress: String?,
    ): Int

    suspend fun resetLoginAttempts(
        email: String,
        userType: UserType,
        ipAddress: String? = null,
    )

    suspend fun lockAccount(
        email: String,
        userType: UserType,
        lockDurationMinutes: Long,
        ipAddress: String? = null,
    ): Boolean

    // OTP
    suspend fun verifyOtp(
        userId: String,
        otp: String,
    ): Boolean

    // Password
    suspend fun changePassword(
        userId: String,
        changePassword: ChangePassword,
    ): Boolean

    suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest): String

    suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult

    // Token refresh
    suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair

    // Logout / Blacklist
    suspend fun logout(
        userId: String,
        refreshToken: String?,
    ): Boolean

    suspend fun blacklistToken(token: String): Boolean

    // Admin
    suspend fun changeUserType(
        currentUserId: String,
        targetUserId: String,
        newUserType: UserType,
    ): Boolean

    suspend fun deactivateUser(
        currentUserId: String,
        targetUserId: String,
    ): Boolean

    suspend fun activateUser(
        currentUserId: String,
        targetUserId: String,
    ): Boolean
}
