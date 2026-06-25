package com.piashcse.feature.auth

import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.database.entities.LoginResponse
import com.piashcse.model.request.ForgetPasswordRequest
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.RefreshTokenRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.request.ResetRequest
import com.piashcse.model.request.TokenPair
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult

interface AuthRepository {
    suspend fun register(registerRequest: RegisterRequest): RegistrationResult
    suspend fun login(loginRequest: LoginRequest): LoginResponse
    suspend fun otpVerification(userId: String, otp: String): Boolean
    suspend fun changePassword(userId: String, changePassword: ChangePassword): Boolean
    suspend fun forgetPassword(forgetPasswordRequest: ForgetPasswordRequest): String
    suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult
    suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair
    suspend fun logout(userId: String, refreshToken: String?): Boolean
    suspend fun blacklistToken(token: String): Boolean
    suspend fun changeUserType(currentUserId: String, targetUserId: String, newUserType: UserType): Boolean
    suspend fun deactivateUser(currentUserId: String, targetUserId: String): Boolean
    suspend fun activateUser(currentUserId: String, targetUserId: String): Boolean
}
