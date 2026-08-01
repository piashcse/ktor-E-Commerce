package com.piashcse.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.event.EventBus
import com.piashcse.event.SendEmailEvent
import com.piashcse.event.UserRegisteredEvent
import com.piashcse.mapper.toUserResponse
import com.piashcse.model.request.ChangePassword
import com.piashcse.model.request.ForgotPasswordRequest
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.LogoutRequest
import com.piashcse.model.request.RefreshTokenRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.request.ResetRequest
import com.piashcse.model.request.TokenPair
import com.piashcse.model.response.LoginResponse
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.InvalidCredentialsException
import com.piashcse.utils.validator.ValidationException

class UserAuthenticationService(private val authRepo: AuthRepository) {

    suspend fun register(registerRequest: RegisterRequest): RegistrationResult {
        val userType = UserType.fromString(registerRequest.userType)
            ?: throw ValidationException(Message.Validation.INVALID_USER_TYPE)
        if (userType == UserType.ADMIN || userType == UserType.SUPER_ADMIN) {
            throw ValidationException(Message.Auth.REGISTRATION_ROLE_FORBIDDEN)
        }
        val result = authRepo.register(registerRequest)
        when (result) {
            is RegistrationResult.Created -> {
                EventBus.publish(
                    UserRegisteredEvent(
                        userId = result.id,
                        email = result.email,
                        userType = registerRequest.userType,
                    ),
                )
                sendRegistrationOtp(result.id, result.email)
            }
            is RegistrationResult.OtpResent -> sendRegistrationOtp(result.id, result.email)
        }
        return result
    }

    private suspend fun sendRegistrationOtp(userId: String, email: String) {
        val otp = authRepo.getRegistrationOtp(userId)
        EventBus.publish(
            SendEmailEvent(
                to = email,
                subject = AppConstants.SmtpServer.OTP_SUBJECT,
                body = "Your verification code is: ${otp.orEmpty()}",
            ),
        )
    }

    suspend fun login(
        loginRequest: LoginRequest,
        ipAddress: String? = null,
    ): LoginResponse {
        val userTypeEnum = UserType.fromString(loginRequest.userType)
            ?: throw ValidationException(Message.Validation.INVALID_USER_TYPE)

        authRepo.getLoginAttempt(loginRequest.email, userTypeEnum)?.let {
            if (it.isLocked) throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES))
            if (it.attemptCount >= AppConstants.Authentication.MAX_LOGIN_ATTEMPTS) {
                authRepo.resetLoginAttempts(loginRequest.email, userTypeEnum)
            }
        }

        val user = authRepo.findUserByEmailAndType(loginRequest.email, userTypeEnum)
        if (user == null) {
            val attemptCount = authRepo.recordFailedAttempt(loginRequest.email, userTypeEnum, ipAddress)
            if (attemptCount >= AppConstants.Authentication.MAX_LOGIN_ATTEMPTS) {
                authRepo.lockAccount(loginRequest.email, userTypeEnum, AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES)
                throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES))
            }
            throw InvalidCredentialsException(remainingAttempts = AppConstants.Authentication.MAX_LOGIN_ATTEMPTS - attemptCount)
        }

        if (!BCrypt.verifyer().verify(loginRequest.password.toCharArray(), user.password).verified) {
            val attemptCount = authRepo.recordFailedAttempt(loginRequest.email, userTypeEnum, ipAddress)
            if (attemptCount >= AppConstants.Authentication.MAX_LOGIN_ATTEMPTS) {
                authRepo.lockAccount(loginRequest.email, userTypeEnum, AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES)
                throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.ACCOUNT_LOCKOUT_MINUTES))
            }
            throw InvalidCredentialsException(remainingAttempts = AppConstants.Authentication.MAX_LOGIN_ATTEMPTS - attemptCount)
        }

        if (!user.isActive) throw ValidationException(Message.Auth.ACCOUNT_DEACTIVATED)
        if (!user.isVerified) throw ValidationException(Message.Auth.ACCOUNT_NOT_VERIFIED)

        authRepo.resetLoginAttempts(loginRequest.email, userTypeEnum)
        val tokenPair = authRepo.generateTokenPair(user.id, user.email, user.userType.name)
        authRepo.storeRefreshToken(user.id, tokenPair.refreshToken)
        return LoginResponse(user.toUserResponse(), tokenPair.accessToken, tokenPair.refreshToken, tokenPair.expiresIn)
    }

    suspend fun otpVerification(userId: String, otp: String): Boolean {
        if (authRepo.isOtpLocked(userId)) {
            throw ValidationException(Message.Auth.accountLocked(AppConstants.Authentication.OTP_LOCKOUT_MINUTES))
        }
        if (authRepo.getOtpAttempt(userId) >= AppConstants.Authentication.MAX_OTP_ATTEMPTS) {
            authRepo.resetOtpAttempts(userId)
        }
        val isValid = authRepo.verifyOtp(userId, otp)
        if (isValid) {
            authRepo.resetOtpAttempts(userId)
        } else {
            val newCount = authRepo.recordFailedOtpAttempt(userId)
            if (newCount >= AppConstants.Authentication.MAX_OTP_ATTEMPTS) {
                authRepo.lockOtpAttempts(userId)
                authRepo.invalidateOtp(userId)
            }
        }
        return isValid
    }

    suspend fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest) {
        val user = authRepo.findResetUserByEmail(forgotPasswordRequest.email, forgotPasswordRequest.userType)
        val otp = authRepo.forgotPassword(forgotPasswordRequest)
        EventBus.publish(
            SendEmailEvent(
                to = user.email,
                subject = AppConstants.SmtpServer.RESET_SUBJECT,
                body = "Your password reset code is: $otp",
            ),
        )
    }

    suspend fun resetPassword(resetPasswordRequest: ResetRequest): ResetResult =
        authRepo.resetPassword(resetPasswordRequest)

    suspend fun refreshAccessToken(request: RefreshTokenRequest): TokenPair =
        authRepo.refreshAccessToken(request)

    suspend fun logout(
        userId: String,
        refreshToken: String?,
    ): Boolean = authRepo.logout(userId, refreshToken)

    suspend fun blacklistToken(token: String): Boolean = authRepo.blacklistToken(token)

    suspend fun changePassword(
        userId: String,
        changePassword: ChangePassword,
    ): Boolean = authRepo.changePassword(userId, changePassword)

    suspend fun changeUserType(
        currentUserId: String,
        targetUserId: String,
        newUserType: UserType,
    ): Boolean = authRepo.changeUserType(currentUserId, targetUserId, newUserType)

    suspend fun deactivateUser(
        currentUserId: String,
        targetUserId: String,
    ): Boolean = authRepo.deactivateUser(currentUserId, targetUserId)

    suspend fun activateUser(
        currentUserId: String,
        targetUserId: String,
    ): Boolean = authRepo.activateUser(currentUserId, targetUserId)
}
