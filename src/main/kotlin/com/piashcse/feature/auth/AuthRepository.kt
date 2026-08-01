package com.piashcse.feature.auth

import com.piashcse.constants.UserType
import com.piashcse.model.domain.AuthUser
import com.piashcse.model.domain.LoginAttemptInfo
import com.piashcse.model.domain.StoredRefreshToken
import com.piashcse.model.request.*
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult

interface AuthRepository {
    // Registration
    suspend fun register(registerRequest: RegisterRequest): RegistrationResult
    suspend fun getRegistrationOtp(userId: String): String?

    // Login
    suspend fun findUserByEmailAndType(email: String, userTypeEnum: UserType): AuthUser?
    suspend fun findUserById(userId: String): AuthUser?
    suspend fun findResetUserByEmail(email: String, userTypeStr: String): AuthUser

    // Token management
    suspend fun storeRefreshToken(userId: String, refreshToken: String)
    suspend fun getRefreshTokenByHash(tokenHash: String): StoredRefreshToken?
    suspend fun revokeRefreshToken(tokenHash: String): Boolean
    suspend fun revokeAllUserTokens(userId: String): Boolean
    fun generateTokenPair(userId: String, email: String, userType: String): TokenPair

    // Login attempt tracking
    suspend fun getLoginAttempt(email: String, userType: UserType): LoginAttemptInfo?
    suspend fun recordFailedAttempt(email: String, userType: UserType, ipAddress: String?): Int
    suspend fun resetLoginAttempts(email: String, userType: UserType)
    suspend fun lockAccount(email: String, userType: UserType, lockDurationMinutes: Long): Boolean

    // OTP
    suspend fun verifyOtp(userId: String, otp: String): Boolean
    suspend fun invalidateOtp(userId: String)

    // OTP attempt tracking
    suspend fun getOtpAttempt(userId: String): Int
    suspend fun isOtpLocked(userId: String): Boolean
    suspend fun recordFailedOtpAttempt(userId: String): Int
    suspend fun resetOtpAttempts(userId: String)
    suspend fun lockOtpAttempts(userId: String)

    // Password
    suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean
    suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest): String
    suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult

    // Token refresh
    suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair

    // Logout / Blacklist
    suspend fun logout(userId: String, refreshToken: String?): Boolean
    suspend fun blacklistToken(token: String): Boolean

    // Admin
    suspend fun changeUserType(currentUserId: String, targetUserId: String, newUserType: UserType): Boolean
    suspend fun deactivateUser(currentUserId: String, targetUserId: String): Boolean
    suspend fun activateUser(currentUserId: String, targetUserId: String): Boolean
}
