package com.piashcse.feature.auth

import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.database.entities.LoginAttemptDAO
import com.piashcse.database.entities.RefreshTokenDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.model.request.ForgotPasswordRequest
import com.piashcse.model.request.RefreshTokenRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.request.ResetRequest
import com.piashcse.model.request.TokenPair
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult

interface AuthRepository {
    // Registration
    suspend fun register(registerRequest: RegisterRequest): RegistrationResult

    // Login
    suspend fun findUserByEmailAndType(email: String, userTypeEnum: UserType): UserDAO?
    suspend fun findUserById(userId: String): UserDAO?
    suspend fun findResetUserByEmail(email: String, userTypeStr: String): UserDAO

    // Token management
    suspend fun storeRefreshToken(userId: String, refreshToken: String)
    suspend fun getRefreshTokenByHash(tokenHash: String): RefreshTokenDAO?
    suspend fun revokeRefreshToken(tokenHash: String): Boolean
    suspend fun revokeAllUserTokens(userId: String): Boolean
    fun generateTokenPair(userId: String, email: String, userType: String): TokenPair

    // Login attempt tracking
    suspend fun getLoginAttempt(email: String, userType: UserType): LoginAttemptDAO?
    suspend fun recordFailedAttempt(email: String, userType: UserType, ipAddress: String?): Int
    suspend fun resetLoginAttempts(email: String, userType: UserType)
    suspend fun lockAccount(email: String, userType: UserType, lockDurationMinutes: Long): Boolean

    // OTP
    suspend fun verifyOtp(userId: String, otp: String): Boolean
    suspend fun invalidateOtp(userId: String)

    // Password
    suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean
    suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest)
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
